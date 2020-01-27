package com.romeikat.datamessie.core.base.ui.page;

import java.util.Collection;

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

import java.util.List;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.time.Duration;
import org.hibernate.SessionFactory;
import org.wicketstuff.modalx.ModalContentPanel;
import org.wicketstuff.modalx.ModalContentWindow;
import org.wicketstuff.modalx.ModalMgr;
import com.romeikat.datamessie.core.base.app.DataMessieSession;
import com.romeikat.datamessie.core.base.dao.impl.ProjectDao;
import com.romeikat.datamessie.core.base.ui.component.NavigationLink;
import com.romeikat.datamessie.core.base.ui.component.ProjectSelector;
import com.romeikat.datamessie.core.base.ui.panel.SidePanel;
import com.romeikat.datamessie.core.base.ui.panel.TaskExecutionsPanel;
import com.romeikat.datamessie.core.domain.dto.ProjectDto;
import com.romeikat.datamessie.core.domain.enums.TaskExecutionStatus;
import jersey.repackaged.com.google.common.collect.Lists;

public abstract class AbstractAuthenticatedPage extends AbstractPage implements ModalMgr {

  private static final long serialVersionUID = 1L;

  private static final int SELF_UPDATING_INTERVAL = 10;

  private DropDownChoice<ProjectDto> aciveProjectDropDownChoice;

  private Link<SignInPage> signOutLink;

  private AjaxLazyLoadPanel ongoingTaskExecutionsPanel;
  private AjaxLazyLoadPanel doneTaskExecutionsPanel;

  private ModalContentWindow[] modalContentWindows;

  private int modalContentWindowAllocated;

  @SpringBean(name = "projectDao")
  private ProjectDao projectDao;

  @SpringBean(name = "sessionFactory")
  private SessionFactory sessionFactory;

  public AbstractAuthenticatedPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();

    provideModalContentWindows();
    initialize();
    // checkForStatelessComponents();
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();

    // If the user is not signed in, redirect him to the sign in page
    if (redirectToSignInPageIfNecessary()) {
      return;
    }
  }

  private boolean redirectToSignInPageIfNecessary() {
    if (!AuthenticatedWebSession.get().isSignedIn()) {
      final AuthenticatedWebApplication app = (AuthenticatedWebApplication) Application.get();
      app.restartResponseAtSignInPage();
      return true;
    }
    return false;
  }

  private void initialize() {
    // Active project
    final IModel<ProjectDto> activeProjectModel = new LoadableDetachableModel<ProjectDto>() {

      private static final long serialVersionUID = 1L;

      @Override
      protected ProjectDto load() {
        // Determine requested project id
        final StringValue projectParameter =
            getRequest().getRequestParameters().getParameterValue("project");
        // Load respective project
        ProjectDto activeProject;
        if (projectParameter.isNull()) {
          activeProject = getDefaultProject();
        } else {
          final Long userId = DataMessieSession.get().getUserId();
          activeProject = projectDao.getAsDto(sessionFactory.getCurrentSession(),
              projectParameter.toLong(), userId);
          if (activeProject == null) {
            activeProject = getDefaultProject();
          }
        }
        // Ensure project parameter
        if (activeProject != null) {
          getPageParameters().set("project", activeProject.getId());
          DataMessieSession.get().getDocumentsFilterSettings().setProjectId(activeProject.getId());
        }
        // Done
        return activeProject;
      }
    };

    aciveProjectDropDownChoice = new ProjectSelector("activeProjectSelector", activeProjectModel) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onSelectionChanged(final ProjectDto newSelection) {
        if (newSelection == null) {
          return;
        }

        final PageParameters projectPageParameters = new PageParameters();
        final Long selectedProjectId = newSelection.getId();
        projectPageParameters.set("project", selectedProjectId);
        final Class<? extends Page> responsePage =
            AbstractAuthenticatedPage.this.getNavigationLinkClass();
        final PageParameters pageParameters = getDefaultPageParameters(projectPageParameters);
        AbstractAuthenticatedPage.this.setResponsePage(responsePage, pageParameters);
      }

      @Override
      protected boolean wantOnSelectionChangedNotifications() {
        return true;
      }
    };
    add(aciveProjectDropDownChoice);

    // Navigation links
    final List<NavigationLink<? extends Page>> navigationLinks =
        getDataMessieApplication().getNavigationLinks();
    final ListView<NavigationLink<? extends Page>> navigationLinksListView =
        new ListView<NavigationLink<? extends Page>>("navigationLinks", navigationLinks) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void populateItem(final ListItem<NavigationLink<? extends Page>> item) {
            // Link
            final NavigationLink<? extends Page> navigationLink = item.getModelObject();
            final PageParameters projectPageParameters = createProjectPageParameters();
            final BookmarkablePageLink<? extends Page> bookmarkablePageLink =
                createBookmarkablePageLink("navigationLink", navigationLink, projectPageParameters);
            final Label bookmarkablePageLinkLabel =
                new Label("navigationLinkLabel", navigationLink.getLabel());
            // Active link
            if (AbstractAuthenticatedPage.this.getNavigationLinkClass() == navigationLink
                .getPageClass()) {
              markLinkSelected(bookmarkablePageLink);
            }
            // Done
            bookmarkablePageLink.add(bookmarkablePageLinkLabel);
            item.add(bookmarkablePageLink);
          }
        };
    add(navigationLinksListView);

    // Sign out link
    signOutLink = new Link<SignInPage>("signOutLink") {
      private static final long serialVersionUID = 1L;

      @Override
      public void onClick() {
        AuthenticatedWebSession.get().invalidate();
        setResponsePage(getApplication().getHomePage());
      }
    };
    add(signOutLink);

    // Side panels
    final List<SidePanel> sidePanels = getDataMessieApplication().getSidePanels();
    final ListView<SidePanel> sidePanelsListView =
        new ListView<SidePanel>("sidePanels", sidePanels) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void populateItem(final ListItem<SidePanel> item) {
            // Link
            final SidePanel sidePanel = item.getModelObject();
            final Panel panel = sidePanel.getPanel();
            item.add(panel);
          }
        };
    add(sidePanelsListView);

    // Ongoing task executions container
    final WebMarkupContainer ongoingTaskExecutionsContainer =
        new WebMarkupContainer("ongoingTaskExecutionsContainer");
    ongoingTaskExecutionsContainer.setOutputMarkupId(true);
    ongoingTaskExecutionsContainer
        .add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(SELF_UPDATING_INTERVAL)));
    add(ongoingTaskExecutionsContainer);

    // Ongoing task executions
    final Collection<TaskExecutionStatus> ongoingStatus =
        Lists.newArrayList(TaskExecutionStatus.EXECUTION_REQUESTED, TaskExecutionStatus.EXECUTING,
            TaskExecutionStatus.IDLE, TaskExecutionStatus.PAUSE_REQUESTED,
            TaskExecutionStatus.PAUSING, TaskExecutionStatus.CANCEL_REQUESTED);
    ongoingTaskExecutionsPanel = new AjaxLazyLoadPanel("ongoingTaskExecutionsPanel") {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getLazyLoadComponent(final String id) {
        final TaskExecutionsPanel ongoingTaskExecutionsPanel =
            new TaskExecutionsPanel(id, ongoingStatus);
        return ongoingTaskExecutionsPanel;
      }

    };
    ongoingTaskExecutionsContainer.add(ongoingTaskExecutionsPanel);

    // Done task executions container
    final WebMarkupContainer doneTaskExecutionsContainer =
        new WebMarkupContainer("doneTaskExecutionsContainer");
    doneTaskExecutionsContainer.setOutputMarkupId(true);
    doneTaskExecutionsContainer
        .add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(SELF_UPDATING_INTERVAL)));
    add(doneTaskExecutionsContainer);

    // Done task executions
    final Collection<TaskExecutionStatus> doneStatus = Lists.newArrayList(
        TaskExecutionStatus.CANCELLED, TaskExecutionStatus.COMPLETED, TaskExecutionStatus.FAILED);
    doneTaskExecutionsPanel = new AjaxLazyLoadPanel("doneTaskExecutionsPanel") {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getLazyLoadComponent(final String id) {
        final TaskExecutionsPanel doneTaskExecutionsPanel = new TaskExecutionsPanel(id, doneStatus);
        return doneTaskExecutionsPanel;
      }

    };
    doneTaskExecutionsContainer.add(doneTaskExecutionsPanel);
  }

  private <T extends Page> BookmarkablePageLink<T> createBookmarkablePageLink(final String id,
      final NavigationLink<T> navigationLink, final PageParameters projectPageParameters) {
    final PageParameters pageParameters = getPageParameters(navigationLink, projectPageParameters);
    final BookmarkablePageLink<T> bookmarkablePageLink =
        new BookmarkablePageLink<T>(id, navigationLink.getPageClass(), pageParameters) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void onConfigure() {
            super.onConfigure();
            final boolean visible =
                navigationLink.isVisible(AbstractAuthenticatedPage.this.getDataMessieSession());
            setVisible(visible);
          }

        };
    return bookmarkablePageLink;
  }

  protected <T extends Page> PageParameters getPageParameters(
      final NavigationLink<T> navigationLink, final PageParameters projectPageParameters) {
    // When navigation from an AbstractDocumentsFilterPage to a different
    // AbstractDocumentsFilterPage, the page parameters should be kept
    final boolean navigatingFromAnAbstractDocumentsFilterPage =
        AbstractDocumentsFilterPage.class.isAssignableFrom(getClass());
    final boolean navigatingToAnAbstractDocumentsFilterPage =
        AbstractDocumentsFilterPage.class.isAssignableFrom(navigationLink.getPageClass());
    final boolean navigatingToADifferentPage = getClass() != navigationLink.getPageClass();
    if (navigatingFromAnAbstractDocumentsFilterPage && navigatingToAnAbstractDocumentsFilterPage
        && navigatingToADifferentPage) {
      final PageParameters currentPageParameters = getPageParameters();
      return currentPageParameters;
    }
    // Otherwise, use the project page parameters and the page parameters specified by the link
    final PageParameters targetPageParameters = projectPageParameters;
    final PageParameters linkPageParameters = navigationLink.getPageParameters();
    if (linkPageParameters != null) {
      targetPageParameters.mergeWith(linkPageParameters);
    }
    return targetPageParameters;
  }

  private void markLinkSelected(final Link<?> link) {
    link.add(new AttributeAppender("class", Model.of("selected")));
  }

  private ProjectDto getDefaultProject() {
    final Long userId = DataMessieSession.get().getUserId();
    final List<ProjectDto> projects =
        projectDao.getAllAsDtos(sessionFactory.getCurrentSession(), userId);
    // No project found
    if (projects.isEmpty()) {
      return null;
    }
    // Some projects found
    final ProjectDto firstProject = projects.get(0);
    return firstProject;
  }

  public IModel<ProjectDto> getActiveProjectModel() {
    return aciveProjectDropDownChoice.getModel();
  }

  public ProjectDto getActiveProject() {
    return aciveProjectDropDownChoice.getModelObject();
  }

  public Long getActiveProjectId() {
    final ProjectDto activeProject = getActiveProject();
    if (activeProject == null) {
      return null;
    }
    return activeProject.getId();
  }

  public PageParameters createProjectPageParameters() {
    // Determine current project parameter
    final Long activeProjectId = getActiveProjectId();
    // Create default parameters
    final PageParameters projectPageParameters = new PageParameters();
    if (activeProjectId != null) {
      projectPageParameters.set("project", activeProjectId);
    }
    // Done
    return projectPageParameters;
  }

  public AjaxLazyLoadPanel getOngoingTaskExecutionsPanel() {
    return ongoingTaskExecutionsPanel;
  }

  public AjaxLazyLoadPanel geDoneTaskExecutionsPanel() {
    return doneTaskExecutionsPanel;
  }

  private void provideModalContentWindows() {
    modalContentWindows = new ModalContentWindow[2];
    for (int i = 0; i < modalContentWindows.length; i++) {
      final String modalContentWindowName = String.format("modalContentWindow%s", i);
      final ModalContentWindow modalContentWindow =
          new ModalContentWindow(this, modalContentWindowName, true);
      modalContentWindow.setAutoSize(true);
      modalContentWindow.setResizable(false);
      modalContentWindow.setCookieName(modalContentWindowName);
      modalContentWindows[i] = modalContentWindow;
      add(modalContentWindow);
    }
    modalContentWindowAllocated = -1;
  }

  @Override
  public ModalContentWindow allocateModalWindow() {
    synchronized (modalContentWindows) {
      if (modalContentWindowAllocated < modalContentWindows.length - 1) {
        modalContentWindowAllocated++;
        return modalContentWindows[modalContentWindowAllocated];
      }
      return null;
    }
  }

  @Override
  public void releaseModalWindow(final ModalContentWindow modalContentWindow) {
    synchronized (modalContentWindow) {
      modalContentWindowAllocated--;
    }
  }

  @Override
  public void preShow(final ModalContentPanel modalContentPanel) {}

  protected Class<? extends Page> getNavigationLinkClass() {
    return getClass();
  }

  protected PageParameters getDefaultPageParameters(final PageParameters projectPageParameters) {
    return projectPageParameters;
  }

}
