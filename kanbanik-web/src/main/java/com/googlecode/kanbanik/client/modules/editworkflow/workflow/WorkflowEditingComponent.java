package com.googlecode.kanbanik.client.modules.editworkflow.workflow;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.DropController;
import com.allen_sauer.gwt.dnd.client.drop.FlowPanelDropController;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.kanbanik.client.KanbanikResources;
import com.googlecode.kanbanik.client.Modules;
import com.googlecode.kanbanik.client.api.DtoFactory;
import com.googlecode.kanbanik.client.api.Dtos;
import com.googlecode.kanbanik.client.messaging.Message;
import com.googlecode.kanbanik.client.messaging.MessageBus;
import com.googlecode.kanbanik.client.messaging.MessageListener;
import com.googlecode.kanbanik.client.messaging.messages.board.BoardChangedMessage;
import com.googlecode.kanbanik.client.modules.editworkflow.workflow.WorkflowEditingComponent.DropDisablingDropController;
import com.googlecode.kanbanik.client.modules.lifecyclelisteners.ModulesLifecycleListener;
import com.googlecode.kanbanik.client.modules.lifecyclelisteners.ModulesLyfecycleListenerHandler;

public class WorkflowEditingComponent extends Composite implements
		ModulesLifecycleListener, MessageListener<Dtos.BoardDto> {

	interface MyUiBinder extends UiBinder<Widget, WorkflowEditingComponent> {
	}
	
	public interface Style extends CssResource {
        String dropTargetStyle();
		String palettePanelStyle();
		String headerTextStyle();
		String tablePanelStyle();
    }


	private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

	@UiField 
	Style style;
	
	@UiField
	Panel board;

	private AbsolutePanel panelWithDraggabls;

	private Dtos.BoardDto boardDto;

	public WorkflowEditingComponent() {
		initWidget(uiBinder.createAndBindUi(this));
		new ModulesLyfecycleListenerHandler(Modules.CONFIGURE, this);
	}

	public void initialize(Dtos.BoardWithProjectsDto boardWithProjects) {
		this.boardDto = boardWithProjects.getBoard();
		renderBoard();
		
		registerListeners();
	}
	
	private void initAndAddPalette(PickupDragController dragController, FlowPanel mainContentPanel) {
		if (boardDto == null) {
			return;
		}
		
		Dtos.WorkflowitemDto horizontal = DtoFactory.workflowitemDto();
		horizontal.setParentWorkflow(boardDto.getWorkflow());
		horizontal.setNestedWorkflow(createNestedWorkflow());
		horizontal.setItemType(Dtos.ItemType.HORIZONTAL.getType());
		horizontal.setName("Horizontal Item");
		horizontal.setWipLimit(-1);
		horizontal.setVerticalSize(-1);
        horizontal.setVersion(1);

        Dtos.WorkflowitemDto vertical = DtoFactory.workflowitemDto();
		vertical.setParentWorkflow(boardDto.getWorkflow());
		vertical.setNestedWorkflow(createNestedWorkflow());
		vertical.setItemType(Dtos.ItemType.VERTICAL.getType());
		vertical.setName("Vertical Item");
		vertical.setWipLimit(-1);
		vertical.setVerticalSize(-1);
        vertical.setVersion(1);
		
		WorkflowItemPalette paletteContent = new WorkflowItemPalette(dragController);
		paletteContent.addWithDraggable(new PaletteWorkflowitemWidget(horizontal, imageResourceAsPanel(KanbanikResources.INSTANCE.rightDropArrowImage())));
		paletteContent.addWithDraggable(new PaletteWorkflowitemWidget(vertical, imageResourceAsPanel(KanbanikResources.INSTANCE.downDropArrowImage())));
		FlowPanel designPanel = new FlowPanel();
		
		Label headerLabel = new Label("Drag and drop workflowitems from palette to workflow");
		headerLabel.setStyleName(style.headerTextStyle());

		designPanel.add(headerLabel);
		designPanel.add(paletteContent);
		designPanel.setStyleName(style.palettePanelStyle());
		
		mainContentPanel.add(designPanel);
	}

	private Dtos.WorkflowDto createNestedWorkflow() {
		Dtos.WorkflowDto dto = DtoFactory.workflowDto();
		dto.setBoard(boardDto);
		return dto;
	}
	
	private Panel imageResourceAsPanel(ImageResource image) {
		Panel panel = new FlowPanel();
		panel.add(new Image(image));
		return panel;
	}
	
	private void renderBoard() {
		if (boardDto == null) {
			return;
		}
		if (panelWithDraggabls != null) {
			board.remove(panelWithDraggabls);
		}
		
		FlexTable table = new FlexTable();
		table.setStyleName("boards-board");
		FlowPanel mainContentPanel = new FlowPanel();
		FlowPanel tableDesignPanel = new FlowPanel();
		tableDesignPanel.addStyleName(style.tablePanelStyle());
		Label headerLabel = new Label("Workflow of board: " + boardDto.getName());
		headerLabel.setStyleName(style.headerTextStyle());
		tableDesignPanel.add(headerLabel);
		tableDesignPanel.add(table);
		
		panelWithDraggabls = new AbsolutePanel();
		PickupDragController dragController = new PickupDragController(panelWithDraggabls, false);
        dragController.setBehaviorDragStartSensitivity(3);
        dragController.setBehaviorCancelDocumentSelections(true);
		
		mainContentPanel.add(tableDesignPanel);
		panelWithDraggabls.add(mainContentPanel);

		buildBoard(null, boardDto.getWorkflow(), null, table,
				dragController, 0, 0);

		// default DTO
		if (boardDto.getWorkflow().getWorkflowitems().isEmpty()) {
			table.setWidget(
					0,
					0,
					createDropTarget(dragController, 
							boardDto.getWorkflow(),
							null, 
							Position.BEFORE,
							KanbanikResources.INSTANCE.insideDropArrowImage()));
		}
		
		board.add(panelWithDraggabls);
		initAndAddPalette(dragController, mainContentPanel);
	}

	public void buildBoard(Dtos.WorkflowDto parentWorkflow,
        Dtos.WorkflowDto currentWorkflow, Dtos.ProjectDto project, FlexTable table,
        PickupDragController dragController, int row, int column) {
    if (currentWorkflow == null || currentWorkflow.getWorkflowitems().isEmpty()) {
        return;
    }

    Dtos.WorkflowitemDto current = currentWorkflow.getWorkflowitems().get(0);
    PositionData positionData = processCurrentItem(currentWorkflow, current, table, dragController, row, column);
    row = positionData.row;
    column = positionData.column;

    for (Dtos.WorkflowitemDto currentItem : currentWorkflow.getWorkflowitems()) {
        processWorkflowItem(currentWorkflow, currentItem, project, table, dragController, row, column);
        PositionData updatedPosition = updatePosition(currentItem, table, currentWorkflow, dragController, row, column);
        row = updatedPosition.row;
        column = updatedPosition.column;
    }
}

private PositionData processCurrentItem(Dtos.WorkflowDto currentWorkflow, Dtos.WorkflowitemDto current, FlexTable table,
        PickupDragController dragController, int row, int column) {
    Dtos.ItemType itemType = Dtos.ItemType.from(current.getItemType());

    switch (itemType) {
        case HORIZONTAL:
            table.setWidget(
                row,
                column,
                createDropTarget(dragController, currentWorkflow,
                        current, Position.BEFORE,
                        KanbanikResources.INSTANCE.rightDropArrowImage()));
            column++;
            break;
        case VERTICAL:
            table.setWidget(
                row,
                column,
                createDropTarget(dragController, currentWorkflow,
                        current, Position.BEFORE,
                        KanbanikResources.INSTANCE.downDropArrowImage()));
            row++;
            break;
        default:
            throw new IllegalStateException("Unsupported item type: '"
                    + current.getItemType() + "'");
    }

    return new PositionData(row, column);
}

private void processWorkflowItem(Dtos.WorkflowDto currentWorkflow, Dtos.WorkflowitemDto currentItem, Dtos.ProjectDto project,
        FlexTable table, PickupDragController dragController, int row, int column) {
    if (!currentItem.getNestedWorkflow().getWorkflowitems().isEmpty()) {
        // Possui um filho
        FlexTable childTable = new FlexTable();
        childTable.setStyleName("boards-board");
        table.setWidget(
                row,
                column,
                createWorkflowitemPlace(dragController, currentItem,
                        project, childTable));
        buildBoard(currentWorkflow, currentItem.getNestedWorkflow(), project,
                childTable, dragController, 0, 0);
    } else {
        // Não possui filho
        Panel dropTarget = createDropTarget(dragController,
                currentItem.getNestedWorkflow(), null, Position.INSIDE,
                KanbanikResources.INSTANCE.insideDropArrowImage());
        table.setWidget(
                row,
                column,
                createWorkflowitemPlace(dragController, currentItem,
                        project, dropTarget));
    }
}

private PositionData updatePosition(Dtos.WorkflowitemDto currentItem, FlexTable table, Dtos.WorkflowDto currentWorkflow,
        PickupDragController dragController, int row, int column) {
    Dtos.ItemType itemType = Dtos.ItemType.from(currentItem.getItemType());

    switch (itemType) {
        case HORIZONTAL:
            column++;
            table.setWidget(
                row,
                column,
                createDropTarget(dragController, currentWorkflow,
                        currentItem, Position.AFTER,
                        KanbanikResources.INSTANCE.rightDropArrowImage()));
            column++;
            break;
        case VERTICAL:
            row++;
            table.setWidget(
                row,
                column,
                createDropTarget(dragController, currentWorkflow,
                        currentItem, Position.AFTER,
                        KanbanikResources.INSTANCE.downDropArrowImage()));
            row++;
            break;
        default:
            throw new IllegalStateException("Unsupported item type: '"
                    + currentItem.getItemType() + "'");
    }

    return new PositionData(row, column);
}

private static class PositionData {
    int row;
    int column;

    PositionData(int row, int column) {
        this.row = row;
        this.column = column;
    }
}

	private Panel createDropTarget(PickupDragController dragController,
			Dtos.WorkflowDto contextItem, Dtos.WorkflowitemDto currentItem,
			Position position, ImageResource image) {
		FlowPanel dropTarget = new FlowPanel();
		dropTarget.setStyleName(style.dropTargetStyle());
		dropTarget.add(new Image(image));
		DropController dropController = new WorkflowEditingDropController(
				dropTarget, contextItem, currentItem, position);
		dragController.registerDropController(dropController);
		return dropTarget;
	}

	private Widget createWorkflowitemPlace(PickupDragController dragController,
			Dtos.WorkflowitemDto currentItem, Dtos.ProjectDto project, Widget childTable) {

		FlowPanel dropTarget = new FlowPanel();
		DropController dropController = new DropDisablingDropController(
				dropTarget);
		dragController.registerDropController(dropController);
		WorkflowitemWidget workflowitemWidget = new WorkflowitemWidget(
				currentItem, childTable);
		dragController.makeDraggable(workflowitemWidget,
				workflowitemWidget.getHeader());
		dropTarget.add(workflowitemWidget);

		return dropTarget;
	}

	enum Position {
		BEFORE, AFTER, INSIDE;
	}

	class DropDisablingDropController extends FlowPanelDropController {

		public DropDisablingDropController(FlowPanel dropTarget) {
			super(dropTarget);
		}

		@Override
		public void onEnter(DragContext context) {
		}

		@Override
		public void onLeave(DragContext context) {
		}

		@Override
		public void onMove(DragContext context) {
		}
	}

	public void activated() {
		registerListeners();
	}

	private void registerListeners() {
		if (!MessageBus.listens(BoardChangedMessage.class, this)) {
			MessageBus.registerListener(BoardChangedMessage.class, this);	
		}
	}

	public void deactivated() {
		unregisterListeners();
	}
	
	public void unregisterListeners() {
		MessageBus.unregisterListener(BoardChangedMessage.class, this);
	}

	public void messageArrived(Message<Dtos.BoardDto> message) {
		boardDto = message.getPayload();
		
		if (boardDto == null) {
			return;
		}
		
		boardDto = message.getPayload();
		renderBoard();
	}
	
}

