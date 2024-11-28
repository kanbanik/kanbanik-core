package com.googlecode.kanbanik.client.components.task;

import com.allen_sauer.gwt.dnd.client.DragController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import com.googlecode.kanbanik.client.api.Dtos;
import com.googlecode.kanbanik.client.messaging.Message;
import com.googlecode.kanbanik.client.messaging.MessageBus;
import com.googlecode.kanbanik.client.messaging.MessageListener;
import com.googlecode.kanbanik.client.messaging.messages.task.ChangeTaskSelectionMessage;
import com.googlecode.kanbanik.client.messaging.messages.task.TaskChangedMessage;
import com.googlecode.kanbanik.client.messaging.messages.task.TaskDeletedMessage;
import com.googlecode.kanbanik.client.messaging.messages.task.TaskEditedMessage;

import java.util.Date;
import java.util.List;

public class TaskGui extends Composite implements MessageListener<Dtos.TaskDto>, ClickHandler {

    @UiField
    FocusPanel header;

    @UiField
    FocusPanel namePanel;

    @UiField
    Label ticketIdLabel;

    @UiField
    HTML dueDateLabel;

    @UiField
    Label nameLabel;

    @UiField
    TextArea nameLabelTextArea;

    @UiField
    PushButton editButton;

    @UiField
    PushButton deleteButton;

    @UiField
    FocusPanel assigneePicturePlace;

    @UiField
    FocusPanel wholePanel;

    @UiField
    FlowPanel mainPanel;

    @UiField
    HTMLPanel contentContainer;

    @UiField
    FlowPanel tagsPanel;

    @UiField
    Style style;

    private static final TaskGuiTemplates TEMPLATE = GWT.create(TaskGuiTemplates.class);

    public static final String SELECTED_STYLE = "selected";

    private Dtos.TaskDto taskDto;

    private TaskManager taskManager;
    private UITaskManager uiTaskManager;
    private EventManager eventManager;

    public TaskGui(Dtos.TaskDto taskDto, Dtos.BoardDto boardDto, DragController dragController) {
        this.taskDto = taskDto;
        this.taskManager = new TaskManager(taskDto);
        this.uiTaskManager = new UITaskManager();
        this.eventManager = new EventManager(this);

        initWidget(GWT.<MyUiBinder>create(MyUiBinder.class).createAndBindUi(this));

        editButton.getUpFace().setImage(new Image("editButtonImage.png"));
        deleteButton.getUpFace().setImage(new Image("deleteButtonImage.png"));

        eventManager.registerListeners();

        wholePanel.addClickHandler(this);

        setupAccordingDto(taskDto);
    }

    public void setupAccordingDto(Dtos.TaskDto taskDto) {
        header.setStyleName("task-class-of-service");
        header.getElement().getStyle().setBackgroundColor(taskManager.getColorOf());
        contentContainer.getElement().getStyle().setBackgroundColor("#ffffff");
        mainPanel.getElement().getStyle().setBackgroundColor("#ffffff");
        ticketIdLabel.setText(taskDto.getTicketId());
        nameLabel.setText(taskDto.getName());
        nameLabel.setTitle(taskDto.getName());
        nameLabelTextArea.setText(taskDto.getName());
        nameLabelTextArea.setTitle(taskDto.getName());

        if (taskDto.getBoard().isFixedSizeShortDescription()) {
            uiTaskManager.setElementDisplay(nameLabel, Display.NONE);
            uiTaskManager.setElementDisplay(nameLabelTextArea, Display.BLOCK);
        } else {
            uiTaskManager.setElementDisplay(nameLabel, Display.TABLE_CELL);
            uiTaskManager.setElementDisplay(nameLabelTextArea, Display.NONE);
        }

        setupDueDate(taskDto.getDueDate());
        setupTags(taskDto.getTaskTags());
    }

    private void setupTags(List<Dtos.TaskTag> tags) {
        tagsPanel.clear();

        if (tags == null) {
            return;
        }

        for (Dtos.TaskTag tag : tags) {
            tagsPanel.add(renderTag(tag));
        }
    }

    private Widget renderTag(final Dtos.TaskTag tag) {
        Widget res;
        String pictureUrl = tag.getPictureUrl();

        if (pictureUrl == null || "".equals(pictureUrl)) {
            FlowPanel tagPanel = new FlowPanel();
            tagPanel.addStyleName(style.tagStyle());
            Label tagLabel = new Label(tag.getName());
            tagLabel.addStyleName(style.tagLabelStyle());
            tagPanel.add(tagLabel);
            res = tagPanel;
        } else {
            final Image tagImage = new Image();
            tagImage.setVisible(false);
            tagImage.addStyleName(style.tagImageStyle());
            tagImage.addLoadHandler(new TagResizingPictureLoadHandler(tagImage));

            tagImage.setUrl(pictureUrl);
            tagImage.setAltText(tagImage.getTitle());
            res = tagImage;
        }

        String color = tag.getColour();

        if (!(color == null || "".equals(color))) {
            res.getElement().getStyle().setBackgroundColor(color);
        }

        String description = tag.getDescription();
        res.setTitle(description != null ? description : "No description provided");

        if (tag.getOnClickUrl() != null && !"".equals(tag.getOnClickUrl())) {
            res.addStyleName(style.clickableTag());
            res.addDomHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    Window.open(tag.getOnClickUrl(), "_blank", "");
                }
            }, ClickEvent.getType());
        }

        return res;
    }

    private void setupDueDate(String dueDate) {
        Date date = taskManager.parseDueDate(dueDate);
        uiTaskManager.setupDueDateLabel(dueDateLabel, dueDate, date, style, TEMPLATE);
    }

    public FocusPanel getHeader() {
        return header;
    }

    public Dtos.TaskDto getDto() {
        return taskDto;
    }

    public TaskSelectionChangeListener getTaskSelectionChangeListener() {
        return new TaskSelectionChangeListener();
    }

    public void doTaskChanged(Message<Dtos.TaskDto> message) {
        Dtos.TaskDto payload = message.getPayload();
        if (payload.getId().equals(taskDto.getId())) {
            this.taskDto = payload;
            setupAccordingDto(payload);
        }
    }

    @Override
    public void onClick(ClickEvent event) {
        eventManager.onClick(event);
    }

    @Override
    public void messageArrived(Message<Dtos.TaskDto> message) {
        eventManager.messageArrived(message);
    }

    public void unregisterListeners() {
        eventManager.unregisterListeners();
    }

    public interface TaskGuiTemplates extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml messageWithLink(String style, String msg);
    }

    public interface Style extends CssResource {
        String selected();

        String unselected();

        String missedStyle();

        String tagStyle();

        String tagLabelStyle();

        String tagImageStyle();

        String clickableTag();
    }

    interface MyUiBinder extends UiBinder<Widget, TaskGui> {}
}