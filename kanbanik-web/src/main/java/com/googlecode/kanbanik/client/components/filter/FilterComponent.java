package com.googlecode.kanbanik.client.components.filter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.kanbanik.client.Modules;
import com.googlecode.kanbanik.client.api.DtoFactory;
import com.googlecode.kanbanik.client.api.Dtos;
import com.googlecode.kanbanik.client.components.DatePickerDialog;
import com.googlecode.kanbanik.client.components.common.filters.CommonFilterCheckBox;
import com.googlecode.kanbanik.client.components.common.DataCollector;
import com.googlecode.kanbanik.client.components.common.filters.PanelWithCheckboxes;
import com.googlecode.kanbanik.client.managers.ClassOfServicesManager;
import com.googlecode.kanbanik.client.managers.TaskTagsManager;
import com.googlecode.kanbanik.client.managers.UsersManager;
import com.googlecode.kanbanik.client.messaging.MessageBus;
import com.googlecode.kanbanik.client.messaging.messages.board.GetAllBoardsResponseMessage;
import com.googlecode.kanbanik.client.messaging.messages.board.GetBoardsRequestMessage;
import com.googlecode.kanbanik.client.messaging.messages.project.GetAllProjectsRequestMessage;
import com.googlecode.kanbanik.client.messaging.messages.project.GetAllProjectsResponseMessage;
import com.googlecode.kanbanik.client.modules.lifecyclelisteners.ModulesLifecycleListener;
import com.googlecode.kanbanik.client.modules.lifecyclelisteners.ModulesLyfecycleListenerHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FilterComponent extends Composite implements ModulesLifecycleListener, BoardsFilter.NumOfHiddenFieldsChangedListener {

    interface MyUiBinder extends UiBinder<Widget, FilterComponent> {}
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    public static final String FILTERS_ACTIVE = "Filtering";

    @UiField
    DisclosurePanel disclosurePanel;

    @UiField
    FilterPanelWithCheckboxes userFilter;

    @UiField
    FilterPanelWithCheckboxes classOfServiceFilter;

    @UiField
    FilterPanelWithCheckboxes boardFilter;

    @UiField
    FilterPanelWithCheckboxes tagsFilter;

    @UiField
    FilterPanelWithCheckboxes projectOnBoardFilter;

    @UiField(provided = true)
    FullTextMatcherFilterComponent fullTextFilter;

    @UiField
    ListBox dueDateCondition;

    DatePickerDialog dueDateFromPicker;

    DatePickerDialog dueDateToPicker;

    @UiField
    TextBox dueDateFromBox;

    @UiField
    TextBox dueDateToBox;

    @UiField
    Label dueDateWarningLabel;

    @UiField
    CheckBox activateFilter;

    private BoardsFilter filterObject;
    private FilterManager filterManager;
    private UIManager uiManager;

    public FilterComponent() {
        fullTextFilter = new FullTextMatcherFilterComponent("Contains Text");

        initWidget(uiBinder.createAndBindUi(this));

        try {
            com.google.gwt.dom.client.Element tr = disclosurePanel.getHeader().getElement().getFirstChildElement().getFirstChildElement();
            com.google.gwt.dom.client.Element labelTd = tr.getFirstChildElement().getNextSiblingElement();
            labelTd.getStyle().setColor("#f4faff");
        } catch (Exception e) {
            // quite risky operation only for changing the color of a label - if it will fail than it is better to ignore it and leave the black there and continue than fail the whole app
        }

        activateFilter.setText(FILTERS_ACTIVE);

        new ModulesLyfecycleListenerHandler(Modules.BOARDS, this);

        filterManager = new FilterManager();
        uiManager = new UIManager();
    }

    private void init() {
        boolean loaded = createFilterObject();

        fullTextFilter.initialize(filterObject, filterObject.getFilterDataDto().getFullTextFilter());

        userFilter.initialize(filterObject);
        classOfServiceFilter.initialize(filterObject);
        boardFilter.initialize(filterObject);
        tagsFilter.initialize(filterObject);
        projectOnBoardFilter.initialize(filterObject);

        filterManager.fillTaskTags(filterObject, loaded, tagsFilter);
        filterManager.fillUsers(filterObject, loaded, userFilter);
        filterManager.fillClassOfServices(filterObject, loaded, classOfServiceFilter);
        filterManager.fillBoards(filterObject, loaded, boardFilter);
        filterManager.fillProjectsOnBoards(filterObject, loaded, projectOnBoardFilter);
        initDueDate(filterObject);
        initActivateFilter(filterObject);

        // using just '|' because I need all the validations to be executed
        if (!fullTextFilter.validate() | !validateDueDate()) {
            return;
        }

        filterObject.fireFilterChangedEvent();
    }

    private void initActivateFilter(final BoardsFilter filterObject) {
        activateFilter.setValue(filterObject.isActive());
        disclosurePanel.setVisible(filterObject.isActive());

        activateFilter.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event) {
                boolean active = event.getValue();
                filterObject.setActive(active);
                disclosurePanel.setVisible(active);
                filterObject.fireFilterChangedEvent();
            }
        });
    }

    private boolean createFilterObject() {
        boolean loaded = true;

        filterObject = new BoardsFilter(this);
        Dtos.FilterDataDto filterDataDto = filterObject.loadFilterData();

        if (filterDataDto == null) {
            filterDataDto = DtoFactory.filterDataDto();

            filterDataDto.setFullTextFilter(DtoFactory.fullTextMatcherDataDto());
            List<Dtos.FilteredEntity> entities = new ArrayList<>();
            filterDataDto.getFullTextFilter().setCaseSensitive(false);
            filterDataDto.getFullTextFilter().setInverse(false);
            filterDataDto.getFullTextFilter().setRegex(false);
            filterDataDto.getFullTextFilter().setString("");

            filterDataDto.getFullTextFilter().setFilteredEntities(entities);

            filterDataDto.setClassesOfServices(new ArrayList<Dtos.ClassOfServiceWithSelectedDto>());
            filterDataDto.setUsers(new ArrayList<Dtos.UserWithSelectedDto>());
            filterDataDto.setBoards(new ArrayList<Dtos.BoardWithSelectedDto>());
            filterDataDto.setBoardWithProjectsDto(new ArrayList<Dtos.BoardWithProjectsWithSelectedDto>());
            filterDataDto.setTaskTags(new ArrayList<Dtos.TaskTagWithSelected>());

            Dtos.DateMatcherDataDto dueDateFilter = DtoFactory.dateMatcherDataDto();
            dueDateFilter.setCondition(0);
            dueDateFilter.setDateFrom("");
            dueDateFilter.setDateTo("");
            filterDataDto.setDueDate(dueDateFilter);

            boolean active = filterDataDto.isActive() != null ? filterDataDto.isActive() : true;
            filterDataDto.setActive(active);

            loaded = false;
        }

        filterObject.setFilterDataDto(filterDataDto);

        return loaded;
    }

    private void initDueDate(BoardsFilter filterObject) {
        dueDateCondition.clear();

        dueDateCondition.addItem("-------");
        dueDateCondition.addItem("due date not set");
        dueDateCondition.addItem("less than");
        dueDateCondition.addItem("equals");
        dueDateCondition.addItem("more than");
        dueDateCondition.addItem("between");

        Dtos.DateMatcherDataDto dueDateMatcher = filterObject.getFilterDataDto().getDueDate();

        dueDateCondition.setSelectedIndex(dueDateMatcher.getCondition());
        setDueDateTextBoxVisibility(dueDateMatcher.getCondition());
        dueDateFromBox.setText(dueDateMatcher.getDateFrom());
        dueDateToBox.setText(dueDateMatcher.getDateTo());

        dueDateFromPicker = new DatePickerDialog(dueDateFromBox) {
            @Override
            public void hide() {
                super.hide();
                setDueDateToFilterObjectAndFireEvent();
            }
        };
        dueDateToPicker = new DatePickerDialog(dueDateToBox) {
            @Override
            public void hide() {
                super.hide();
                setDueDateToFilterObjectAndFireEvent();
            }
        };

        dueDateFromBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dueDateFromPicker.show();
            }
        });

        dueDateToBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dueDateToPicker.show();
            }
        });

        dueDateCondition.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                setDueDateTextBoxVisibility(dueDateCondition.getSelectedIndex());

                setDueDateToFilterObjectAndFireEvent();
            }
        });


        dueDateFromBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                setDueDateToFilterObjectAndFireEvent();
            }
        });

        dueDateToBox.addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                setDueDateToFilterObjectAndFireEvent();
            }
        });

    }

    private void setDueDateTextBoxVisibility(int condition) {
        if (condition == BoardsFilter.DATE_CONDITION_BETWEEN) {
            dueDateToBox.setVisible(true);
        } else {
            dueDateToBox.setVisible(false);
        }

        if (condition == BoardsFilter.DATE_CONDITION_UNSET || condition == BoardsFilter.DATE_CONDITION_ONLY_WITHOUT) {
            dueDateToBox.setVisible(false);
            dueDateFromBox.setVisible(false);
        } else {
            dueDateFromBox.setVisible(true);
        }
    }

    private void setDueDateToFilterObjectAndFireEvent() {
        if (!validateDueDate()) {
            return;
        }

        Dtos.DateMatcherDataDto dueDateMatches = DtoFactory.dateMatcherDataDto();
        dueDateMatches.setCondition(dueDateCondition.getSelectedIndex());

        dueDateMatches.setDateFrom(dueDateFromBox.getText());
        dueDateMatches.setDateTo(dueDateToBox.getText());
        filterObject.getFilterDataDto().setDueDate(dueDateMatches);
        filterObject.fireFilterChangedEvent();
    }

    private boolean validateDueDate() {
        dueDateWarningLabel.setText("");

        int dateCondition = dueDateCondition.getSelectedIndex();

        if (dateCondition == BoardsFilter.DATE_CONDITION_UNSET) {
            return true;
        }

        boolean fromFilled = filterObject.parseDate(dueDateFromBox.getText()) != null;
        boolean toFilled = filterObject.parseDate(dueDateToBox.getText()) != null;

        if ((dateCondition == BoardsFilter.DATE_CONDITION_LESS ||
                dateCondition == BoardsFilter.DATE_CONDITION_MORE ||
                dateCondition == BoardsFilter.DATE_CONDITION_EQALS
        ) && !fromFilled) {
            dueDateWarningLabel.setText("Please fill the date");
            return false;
        }

        if (dateCondition == BoardsFilter.DATE_CONDITION_BETWEEN && (!fromFilled || !toFilled)) {
            dueDateWarningLabel.setText("Please fill the date");
            return false;
        }

        return true;
    }

    @Override
    public void activated() {
        init();
    }

    @Override
    public void deactivated() {

    }

    @Override
    public void onNumOfHiddenFieldsChanged(int newNum) {
        // if (!activateFilter.getValue() || newNum == 0) {
        //     activateFilter.setText(FILTERS_ACTIVE);
        // } else {
        //     activateFilter.setText(FILTERS_ACTIVE + " (" + newNum + " entities match criteria to be hidden)");
        // }
    }
}