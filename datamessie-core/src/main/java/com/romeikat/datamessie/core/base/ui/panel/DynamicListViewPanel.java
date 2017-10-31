package com.romeikat.datamessie.core.base.ui.panel;

/*-
 * ============================LICENSE_START============================
 * data.messie (core)
 * =====================================================================
 * Copyright (C) 2013 - 2017 Dr. Raphael Romeikat
 * =====================================================================
 * This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public
License along with this program.  If not, see
<http://www.gnu.org/licenses/gpl-3.0.html>.
 * =============================LICENSE_END=============================
 */

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public abstract class DynamicListViewPanel<T extends Serializable> extends Panel {

  private static final long serialVersionUID = 1L;

  private final IModel<List<T>> itemModel;

  private final MarkupContainer itemsListContainer;

  private final ListView<T> itemsList;

  public DynamicListViewPanel(final String id, final IModel<List<T>> itemsModel) {
    super(id);
    // Rules
    this.itemModel = itemsModel;
    // Container for the ListView (needed to update the ListView with Ajax)
    itemsListContainer = new WebMarkupContainer("itemsListContainer");
    itemsListContainer.setOutputMarkupId(true);
    add(itemsListContainer);
    // One line per rule
    final List<T> items = itemsModel.getObject();
    itemsList = new ListView<T>("itemsList", items) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void populateItem(final ListItem<T> listItem) {
        // Item
        final IModel<T> itemModel = listItem.getModel();
        final Panel itemPanel = DynamicListViewPanel.this.getItemPanel("itemPanel", itemModel);
        listItem.add(itemPanel);
        // Links for editing the list
        listItem.add(createRemoveLink(listItem));
        listItem.add(createMoveDownLink(listItem));
        listItem.add(createMoveUpLink(listItem));
      }
    };
    itemsList.setReuseItems(true);
    itemsListContainer.add(itemsList);
    // Link for adding a new rule
    add(createAddLink());
  }

  private AjaxLink<Void> createRemoveLink(final ListItem<T> listItem) {
    final AjaxLink<Void> removeLink = new AjaxLink<Void>("removeLink") {
      private static final long serialVersionUID = 1L;

      @Override
      public void onClick(final AjaxRequestTarget target) {
        final int index = listItem.getIndex();
        // Get current list
        final List<T> items = itemModel.getObject();
        // Remove rule
        items.remove(index);
        // Update rules
        itemsList.setList(items);
        // Update model
        itemModel.setObject(items);
        // Render view
        target.add(itemsListContainer);
      }
    };
    return removeLink;
  }

  private AjaxLink<Void> createMoveDownLink(final ListItem<T> listItem) {
    final AjaxLink<Void> moveDownLink = new AjaxLink<Void>("moveDownLink") {
      private static final long serialVersionUID = 1L;

      @Override
      public void onClick(final AjaxRequestTarget target) {
        final int index = listItem.getIndex();
        // Get current list
        final List<T> items = itemModel.getObject();
        // Swap rules
        Collections.swap(items, index, index + 1);
        // Update rules
        itemsList.setList(items);
        // Update model
        itemModel.setObject(items);
        // Render view
        target.add(itemsListContainer);
      }

      @Override
      public void onConfigure() {
        final List<T> items = itemModel.getObject();
        final boolean visible = listItem.getIndex() != items.size() - 1;
        setVisible(visible);
      }
    };
    return moveDownLink;
  }

  private AjaxLink<Void> createMoveUpLink(final ListItem<T> listItem) {
    final AjaxLink<Void> moveUpLink = new AjaxLink<Void>("moveUpLink") {
      private static final long serialVersionUID = 1L;

      @Override
      public void onClick(final AjaxRequestTarget target) {
        final int index = listItem.getIndex();
        // Get current list
        final List<T> items = itemModel.getObject();
        // Swap rules
        Collections.swap(items, index, index - 1);
        // Update rules
        itemsList.setList(items);
        // Update model
        itemModel.setObject(items);
        // Render view
        target.add(itemsListContainer);
      }

      @Override
      public void onConfigure() {
        final boolean visible = listItem.getIndex() != 0;
        setVisible(visible);
      }
    };
    return moveUpLink;
  }

  private AjaxLink<Void> createAddLink() {
    final AjaxLink<Void> addLink = new AjaxLink<Void>("addLink") {
      private static final long serialVersionUID = 1L;

      @Override
      public void onClick(final AjaxRequestTarget target) {
        final T item = DynamicListViewPanel.this.newItem();
        // Get rules
        final List<T> items = itemModel.getObject();
        // Add new rule
        items.add(item);
        // Update rules
        itemsList.setList(items);
        // Update model
        itemModel.setObject(items);
        // Render view
        target.add(itemsListContainer);
      }
    };
    return addLink;
  }

  protected abstract Panel getItemPanel(String id, IModel<T> itemModel);

  protected abstract T newItem();

  @Override
  protected void onDetach() {
    super.onDetach();

    itemModel.detach();
  }

}
