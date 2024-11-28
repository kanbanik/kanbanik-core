package com.googlecode.kanbanik.client.components;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.*;

/**
 * Extension of the standard GWT DialogBox to provide a more "window"-like functionality.
 */
public class WindowBox extends DialogBox implements HasOpenHandlers<WindowBox> {

    private static final int MIN_WIDTH = 100;
    private static final int MIN_HEIGHT = 100;

    private FlowPanel container;
    private FlowPanel controls;
    private Anchor close;
    private Anchor minimize;

    private int dragX;
    private int dragY;

    private int minWidth = MIN_WIDTH;
    private int minHeight = MIN_HEIGHT;

    private int dragMode;

    private boolean resizable;

    private boolean minimized;

    public WindowBox() {
        this(false, false, true, true, false);
    }

    public WindowBox(boolean resizable) {
        this(false, false, true, true, resizable);
    }

    public WindowBox(boolean showCloseIcon, boolean resizable) {
        this(false, false, true, showCloseIcon, resizable);
    }

    public WindowBox(boolean showMinimizeIcon, boolean showCloseIcon, boolean resizable) {
        this(false, false, showMinimizeIcon, showCloseIcon, resizable);
    }

    public WindowBox(boolean modal, boolean showMinimizeIcon, boolean showCloseIcon, boolean resizable) {
        this(false, modal, showMinimizeIcon, showCloseIcon, resizable);
    }

    public WindowBox(boolean autoHide, boolean modal, boolean showMinimizeIcon, boolean showCloseIcon, boolean resizable) {
        super(autoHide, modal);
        initializeWindow(showMinimizeIcon, showCloseIcon, resizable);
    }

    private void initializeWindow(boolean showMinimizeIcon, boolean showCloseIcon, boolean resizable) {
        setStyleName("gwt-extras-WindowBox", true);

        container = new FlowPanel();
        container.addStyleName("gwt-extras-dialog-container");

        close = createCloseAnchor(showCloseIcon);
        minimize = createMinimizeAnchor(showMinimizeIcon);

        Grid ctrlGrid = new Grid(1, 2);
        ctrlGrid.setWidget(0, 0, minimize);
        ctrlGrid.setWidget(0, 1, close);

        controls = new FlowPanel();
        controls.setStyleName("gwt-extras-dialog-controls");
        controls.add(ctrlGrid);
        dragMode = -1;

        this.resizable = resizable;
    }

    private Anchor createCloseAnchor(boolean showCloseIcon) {
        Anchor closeAnchor = new Anchor();
        closeAnchor.setStyleName("gwt-extras-dialog-close");
        closeAnchor.addClickHandler(this::onCloseClick);
        closeAnchor.setVisible(showCloseIcon);
        return closeAnchor;
    }

    private Anchor createMinimizeAnchor(boolean showMinimizeIcon) {
        Anchor minimizeAnchor = new Anchor();
        minimizeAnchor.setStyleName("gwt-extras-dialog-minimize");
        minimizeAnchor.addClickHandler(this::onMinimizeClick);
        minimizeAnchor.setVisible(showMinimizeIcon);
        return minimizeAnchor;
    }

    public boolean isResizable() {
        return this.resizable;
    }

    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (resizable && handleResizeEvents(event)) {
            return;
        }
        super.onBrowserEvent(event);
    }

    private boolean handleResizeEvents(Event event) {
        switch (event.getTypeInt()) {
            case Event.ONMOUSEDOWN:
            case Event.ONMOUSEUP:
            case Event.ONMOUSEMOVE:
            case Event.ONMOUSEOVER:
            case Event.ONMOUSEOUT:
                if (dragMode >= 0 || calcDragMode(event.getClientX(), event.getClientY()) >= 0) {
                    DomEvent.fireNativeEvent(event, this, getElement());
                    return true;
                }
                if (dragMode < 0) {
                    updateCursor(dragMode);
                }
                break;
        }
        return false;
    }

    private void updateCursor(int dragMode) {
        if (resizable) {
            Cursor cursor = getCursorForDragMode(dragMode);
            updateElementCursor(getElement(), cursor);
            com.google.gwt.dom.client.Element top = getCellElement(0, 1);
            updateElementCursor(top, cursor);
            top = Element.as(top.getFirstChild());
            if (top != null) {
                updateElementCursor(top, cursor);
            }
        }
    }

    private void updateElementCursor(com.google.gwt.dom.client.Element element, Cursor cursor) {
        element.getStyle().setCursor(cursor);
    }

    private Cursor getCursorForDragMode(int dragMode) {
        switch (dragMode) {
            case 0:
                return Cursor.NW_RESIZE;
            case 1:
                return Cursor.N_RESIZE;
            case 2:
                return Cursor.NE_RESIZE;
            case 3:
                return Cursor.W_RESIZE;
            case 5:
                return Cursor.E_RESIZE;
            case 6:
                return Cursor.SW_RESIZE;
            case 7:
                return Cursor.S_RESIZE;
            case 8:
                return Cursor.SE_RESIZE;
            default:
                return Cursor.DEFAULT;
        }
    }

    private int calcDragMode(int clientX, int clientY) {
        if (isInResizeArea(clientX, clientY, getCellElement(2, 2).getParentElement(), -5, -5)) {
            return 8;
        } else if (isInResizeArea(clientX, clientY, getCellElement(2, 0).getParentElement(), 5, -5)) {
            return 6;
        } else if (isInResizeArea(clientX, clientY, getCellElement(0, 2).getParentElement(), -5, 5)) {
            return 2;
        } else if (isInResizeArea(clientX, clientY, getCellElement(0, 0).getParentElement(), 5, 5)) {
            return 0;
        } else if (isInResizeLine(clientX, clientY, getCellElement(0, 1).getParentElement())) {
            return 1;
        } else if (isInResizeLine(clientX, clientY, getCellElement(1, 0).getParentElement())) {
            return 3;
        } else if (isInResizeLine(clientX, clientY, getCellElement(2, 1).getParentElement())) {
            return 7;
        } else if (isInResizeLine(clientX, clientY, getCellElement(1, 2).getParentElement())) {
            return 5;
        }
        return -1;
    }

    private boolean isInResizeArea(int clientX, int clientY, com.google.gwt.dom.client.Element resize, int xOffset, int yOffset) {
        int xr = getRelativeX(resize, clientX);
        int yr = getRelativeY(resize, clientY);
        int w = resize.getClientWidth();
        int h = resize.getClientHeight();
        return (xr >= xOffset && xr < w && yr >= yOffset && yr < h);
    }

    private boolean isInResizeLine(int clientX, int clientY, com.google.gwt.dom.client.Element resize) {
        int xr = getRelativeX(resize, clientX);
        int yr = getRelativeY(resize, clientY);
        int w = resize.getClientWidth();
        int h = resize.getClientHeight();
        return (xr >= 0 && xr < w && yr >= 0 && yr < h);
    }

    private int getRelativeX(com.google.gwt.dom.client.Element element, int clientX) {
        return clientX - element.getAbsoluteLeft() + element.getScrollLeft() + element.getOwnerDocument().getScrollLeft();
    }

    private int getRelativeY(com.google.gwt.dom.client.Element element, int clientY) {
        return clientY - element.getAbsoluteTop() + element.getScrollTop() + element.getOwnerDocument().getScrollTop();
    }

    private void dragResizeWidget(PopupPanel panel, int dx, int dy) {
        int x = getPopupLeft();
        int y = getPopupTop();

        Widget widget = panel.getWidget();

        x = adjustWidth(dx, x, widget);
        y = adjustHeight(dy, y, widget);

        panel.setPopupPosition(x, y);
    }

    private int adjustWidth(int dx, int x, Widget widget) {
        if ((dragMode % 3) != 1) {
            int w = widget.getOffsetWidth();
            if ((dragMode % 3) == 0) {
                x += dx;
                w -= dx;
            } else {
                w += dx;
            }
            w = Math.max(w, minWidth);
            widget.setWidth(w + "px");
        }
        return x;
    }

    private int adjustHeight(int dy, int y, Widget widget) {
        if ((dragMode / 3) != 1) {
            int h = widget.getOffsetHeight();
            if ((dragMode / 3) == 0) {
                y += dy;
                h -= dy;
            } else {
                h += dy;
            }
            h = Math.max(h, minHeight);
            widget.setHeight(h + "px");
        }
        return y;
    }

    @Override
    protected void beginDragging(MouseDownEvent event) {
        if (resizable && !minimized) {
            int dm = calcDragMode(event.getClientX(), event.getClientY());
            if (dm >= 0) {
                startResizing(event, dm);
                return;
            }
        }
        super.beginDragging(event);
    }

    private void startResizing(MouseDownEvent event, int dm) {
        dragMode = dm;
        DOM.setCapture(getElement());
        dragX = event.getClientX();
        dragY = event.getClientY();
        updateElementCursor(RootPanel.get().getElement(), getCursorForDragMode(dm));
    }

    @Override
    protected void continueDragging(MouseMoveEvent event) {
        if (dragMode >= 0 && resizable) {
            updateCursor(dragMode);
            int dx = event.getClientX() - dragX;
            int dy = event.getClientY() - dragY;
            dragX = event.getClientX();
            dragY = event.getClientY();
            dragResizeWidget(this, dx, dy);
        } else {
            if (!minimized) {
                int dm = calcDragMode(event.getClientX(), event.getClientY());
                updateCursor(dm);
            }
            super.continueDragging(event);
        }
    }

    @Override
    protected void endDragging(MouseUpEvent event) {
        if (dragMode >= 0 && resizable) {
            stopResizing(event);
        } else {
            super.endDragging(event);
        }
    }

    private void stopResizing(MouseUpEvent event) {
        DOM.releaseCapture(getElement());
        dragMode = -1;
        updateCursor(dragMode);
        RootPanel.get().getElement().getStyle().setCursor(Cursor.DEFAULT);
    }

    @Override
    protected void onPreviewNativeEvent(NativePreviewEvent event) {
        if (resizable) {
            NativeEvent nativeEvent = event.getNativeEvent();
            if (!event.isCanceled()
                    && event.getTypeInt() == Event.ONMOUSEDOWN
                    && calcDragMode(nativeEvent.getClientX(), nativeEvent.getClientY()) >= 0) {
                nativeEvent.preventDefault();
            }
        }
        super.onPreviewNativeEvent(event);
    }

    @Override
    public void setWidget(Widget widget) {
        if (container.getWidgetCount() == 0) {
            container.add(controls);
            super.setWidget(container);
        } else {
            if (container.getWidgetCount() > 1) {
                container.remove(1);
            }
        }
        container.add(widget);
    }

    @Override
    public Widget getWidget() {
        if (container.getWidgetCount() > 1) {
            return container.getWidget(1);
        } else {
            return null;
        }
    }

    @Override
    public boolean remove(Widget w) {
        return container.remove(w);
    }

    public void setCloseIconVisible(boolean visible) {
        close.setVisible(visible);
    }

    public void setMinimizeIconVisible(boolean visible) {
        minimize.setVisible(visible);
    }

    public FlowPanel getControlPanel() {
        return controls;
    }

    protected void onCloseClick(ClickEvent event) {
        hide();
    }

    protected void onMinimizeClick(ClickEvent event) {
        Widget widget = getWidget();

        if (widget == null) {
            return;
        }

        boolean visible = widget.isVisible();

        int offsetWidth = widget.getOffsetWidth();

        widget.setVisible(!visible);
        minimized = visible;

        if (visible) {
            container.setWidth(offsetWidth + "px");
            minimize.setStyleName("gwt-extras-dialog-maximize");
        } else {
            container.setWidth(null);
            minimize.setStyleName("gwt-extras-dialog-minimize");
        }
    }

    @Override
    public HandlerRegistration addOpenHandler(OpenHandler<WindowBox> handler) {
        return addHandler(handler, OpenEvent.getType());
    }

    @Override
    public void show() {
        boolean fireOpen = !isShowing();
        super.show();
        if (fireOpen) {
            OpenEvent.fire(this, this);
        }
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = Math.max(minWidth, MIN_WIDTH);
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = Math.max(minHeight, MIN_HEIGHT);
    }
}