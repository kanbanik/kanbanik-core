package com.googlecode.kanbanik.client.components.filter;

import com.googlecode.kanbanik.client.api.Dtos;
import com.googlecode.kanbanik.client.components.common.filters.CommonFilterCheckBox;
import com.googlecode.kanbanik.client.components.common.filters.PanelWithCheckboxes;
import com.googlecode.kanbanik.client.managers.ClassOfServicesManager;
import com.googlecode.kanbanik.client.managers.TaskTagsManager;
import com.googlecode.kanbanik.client.managers.UsersManager;
import com.googlecode.kanbanik.client.messaging.MessageBus;
import com.googlecode.kanbanik.client.messaging.messages.board.GetAllBoardsResponseMessage;
import com.googlecode.kanbanik.client.messaging.messages.board.GetBoardsRequestMessage;
import com.googlecode.kanbanik.client.messaging.messages.project.GetAllProjectsRequestMessage;
import com.googlecode.kanbanik.client.messaging.messages.project.GetAllProjectsResponseMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JLabel;

import com.googlecode.kanbanik.client.api.DtoFactory;
import com.googlecode.kanbanik.client.components.common.DataCollector;
import com.googlecode.kanbanik.client.messaging.messages.board.GetBoardsRequestMessage.Filter;

public class FilterManager {
    private BoardsFilter filterObject;
    private DataCollector<Dtos.BoardDto> boardsCollector = new DataCollector<>();
    private DataCollector<Dtos.BoardWithProjectsDto> projectsOnBoardsCollector = new DataCollector<>();

    public FilterManager() {
        this.filterObject = filterObject;
    }

    public void addFilter(Filter filter) {
        // Lógica para adicionar filtro
    }

    public boolean applyFilters(int dateCondition, String dueDateFrom, String dueDateTo, JLabel dueDateWarningLabel) {
        if (dateCondition == BoardsFilter.DATE_CONDITION_UNSET) {
            return true;
        }

        boolean fromFilled = filterObject.parseDate(dueDateFrom) != null;
        boolean toFilled = filterObject.parseDate(dueDateTo) != null;

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

        // Lógica adicional para aplicar filtros
        return true;
    }

    public void fillUsers(final boolean loaded) {
        List<Dtos.UserDto> users = UsersManager.getInstance().getUsers();

        users.add(0, UsersManager.getInstance().getNoUser());

        for (Dtos.UserDto user : users) {
            addUser(loaded, user);
        }

        UsersManager.getInstance().setListener(new UsersManager.UserChangedListener() {
            @Override
            public void added(Dtos.UserDto user) {
                addUser(loaded, user);
                filterObject.fireFilterChangedEvent();
            }
        });
    }

    private void addUser(boolean loaded, Dtos.UserDto user) {
        if (!loaded || filterObject.findById(user) == -1) {
            filterObject.add(user);
        }
        userFilter.add(new UserFilterCheckBox(user, filterObject));
    }

    public void fillClassOfServices(final boolean loaded) {
        List<Dtos.ClassOfServiceDto> sorted = new ArrayList<>(ClassOfServicesManager.getInstance().getAllWithNone());

        Collections.sort(sorted, new Comparator<Dtos.ClassOfServiceDto>() {
            @Override
            public int compare(Dtos.ClassOfServiceDto classOfServiceDto, Dtos.ClassOfServiceDto classOfServiceDto2) {
                return classOfServiceDto.getName().compareTo(classOfServiceDto2.getName());
            }
        });

        // add default class of service if not present
        if (sorted.isEmpty() || sorted.get(0).getId() != null) {
            sorted.add(0, ClassOfServicesManager.getInstance().getDefaultClassOfService());
        }

        for (Dtos.ClassOfServiceDto classOfServiceDto : sorted) {
            addClassOfService(loaded, classOfServiceDto);
        }

        ClassOfServicesManager.getInstance().setListener(new ClassOfServicesManager.ClassOfServiceChangedListener() {
            @Override
            public void added(Dtos.ClassOfServiceDto classOfServiceDto) {
                addClassOfService(loaded, classOfServiceDto);
                filterObject.fireFilterChangedEvent();
            }
        });
    }

    private void addClassOfService(boolean loaded, Dtos.ClassOfServiceDto classOfServiceDto) {
        if (!loaded || filterObject.findById(classOfServiceDto) == -1) {
            filterObject.add(classOfServiceDto);
        }
        classOfServiceFilter.add(new ClassOfServiceFilterCheckBox(classOfServiceDto, filterObject));
    }

    public void fillBoards(boolean loaded) {
        MessageBus.unregisterListener(GetAllBoardsResponseMessage.class, boardsCollector);
        MessageBus.registerListener(GetAllBoardsResponseMessage.class, boardsCollector);
        boardsCollector.init();
        MessageBus.sendMessage(new GetBoardsRequestMessage(null, new GetBoardsRequestMessage.Filter() {
            @Override
            public boolean apply(Dtos.BoardDto boardDto) {
                return true;
            }
        }, this));

        List<Dtos.BoardDto> boards = boardsCollector.getData();
        List<Dtos.BoardDto> shallowBoards = new ArrayList<>();
        for (Dtos.BoardDto board : boards) {
            shallowBoards.add(asShallowBoard(board));
        }

        Collections.sort(shallowBoards, new Comparator<Dtos.BoardDto>() {
            @Override
            public int compare(Dtos.BoardDto b1, Dtos.BoardDto b2) {
                return b1.getName().compareTo(b2.getName());
            }
        });

        for (Dtos.BoardDto board : shallowBoards) {
            if (!loaded || filterObject.findById(board) == -1) {
                filterObject.add(board);
            }
            boardFilter.add(new BoardsFilterCheckBox(board, filterObject));
        }
    }

    public void fillTaskTags(BoardsFilter filterObject1, final boolean loaded, FilterPanelWithCheckboxes tagsFilter) {
        List<Dtos.TaskTag> tags = TaskTagsManager.getInstance().getTags();

        Collections.sort(tags, new Comparator<Dtos.TaskTag>() {
            @Override
            public int compare(Dtos.TaskTag t1, Dtos.TaskTag t2) {
                return t1.getName().compareTo(t2.getName());
            }
        });

        if (tags.isEmpty() || tags.get(0).getId() != null) {
            tags.add(0, TaskTagsManager.getInstance().noTag());
        }

        if (filterObject.getFilterDataDto().getTaskTags() == null) {
            filterObject.getFilterDataDto().setTaskTags(new ArrayList<Dtos.TaskTagWithSelected>());
        }

        for (Dtos.TaskTag tag : tags) {
            addTag(tag, loaded);
        }

        TaskTagsManager.getInstance().setListener(new TaskTagsManager.TagsChangedListener() {
            @Override
            public void added(Dtos.TaskTag tag) {
                addTag(tag, loaded);
                filterObject.fireFilterChangedEvent();
            }

            @Override
            public void removed(Dtos.TaskTag tag) {
                removeTag(tag);
            }
        });
    }

    private void removeTag(final Dtos.TaskTag tag) {
        filterObject.deleteFromStorage(tag);

        tagsFilter.remove(new PanelWithCheckboxes.Predicate() {
            @Override
            public boolean toRemove(CommonFilterCheckBox w) {
                Dtos.TaskTag candidate = (Dtos.TaskTag) w.getEntity();
                return objEq(candidate.getName(), tag.getName());
            }
        });

        filterObject.storeFilterData();
    }

    private boolean objEq(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }

        if (o2 == null) {
            return false;
        }

        return o1.equals(o2);
    }

    private void addTag(Dtos.TaskTag tag, boolean loaded) {
        if (!loaded || filterObject.findByName(tag) == -1) {
            filterObject.add(tag);
        }

        tagsFilter.add(new TaskTagFilterCheckBox(tag, filterObject));
    }

    private Dtos.BoardDto asShallowBoard(Dtos.BoardDto board) {
        Dtos.BoardDto shallowBoard = DtoFactory.boardDto();
        shallowBoard.setId(board.getId());
        shallowBoard.setName(board.getName());
        return shallowBoard;
    }

    public void fillProjectsOnBoards(boolean loaded) {
        MessageBus.unregisterListener(GetAllProjectsResponseMessage.class, projectsOnBoardsCollector);
        MessageBus.registerListener(GetAllProjectsResponseMessage.class, projectsOnBoardsCollector);
        projectsOnBoardsCollector.init();
        MessageBus.sendMessage(new GetAllProjectsRequestMessage(null, this));

        List<Dtos.BoardWithProjectsDto> boardsWithProjectsDtos = projectsOnBoardsCollector.getData();

        List<Dtos.BoardWithProjectsDto> shallowBoardsWithProjectsDtos = new ArrayList<>();
        for (Dtos.BoardWithProjectsDto boardWithProjectsDto : boardsWithProjectsDtos) {
            Dtos.BoardWithProjectsDto shallowBoardWithProjectsDto = DtoFactory.boardWithProjectsDto();
            shallowBoardWithProjectsDto.setBoard(asShallowBoard(boardWithProjectsDto.getBoard()));
            shallowBoardWithProjectsDto.setProjectsOnBoard(boardWithProjectsDto.getProjectsOnBoard());
            shallowBoardsWithProjectsDtos.add(shallowBoardWithProjectsDto);
        }

        Collections.sort(shallowBoardsWithProjectsDtos, new Comparator<Dtos.BoardWithProjectsDto>() {
            @Override
            public int compare(Dtos.BoardWithProjectsDto b1, Dtos.BoardWithProjectsDto b2) {
                return b1.getProjectsOnBoard().getValues().get(0).getName().compareTo(b2.getProjectsOnBoard().getValues().get(0).getName());
            }
        });

        for (Dtos.BoardWithProjectsDto boardWithProjectDtos : shallowBoardsWithProjectsDtos) {
            if (!loaded || filterObject.findById(boardWithProjectDtos) == -1) {
                filterObject.add(boardWithProjectDtos);
            }
            projectOnBoardFilter.add(new ProjectOnBoardFilterCheckBox(boardWithProjectDtos, filterObject));
        }
    }
}