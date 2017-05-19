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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxFallbackButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.modalx.ModalContentPanel;
import org.wicketstuff.modalx.ModalContentWindow;

import com.romeikat.datamessie.core.base.ui.page.AbstractAuthenticatedPage;
import com.romeikat.datamessie.core.base.util.FileProcessingException;

public abstract class FileUploadPanel extends ModalContentPanel {

  private static final Logger LOG = LoggerFactory.getLogger(FileUploadPanel.class);

  private static final long serialVersionUID = 1L;

  private static Panel inputPanel;

  private Form<Void> uploadForm;

  private FileUploadField[] fileUploadFields;

  private final IModel<String> feedbackModel = new Model<String>();

  private TextArea<String> feedbackTextArea;

  public FileUploadPanel(final ModalContentWindow modalContentWindow) {
    super(modalContentWindow, null);
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    initialize();
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();
    if (inputPanel == null || inputPanel.getClass() == EmptyPanel.class) {
      inputPanel.setVisible(false);
    }
  }

  private void initialize() {
    setTitle("File Upload");

    uploadForm = new Form<Void>("uploadForm");
    uploadForm.setMultiPart(true);
    add(uploadForm);

    // Input panel (only create once, so the same panel will be reused for future versions of
    // the FileUplodPanel)
    if (inputPanel == null) {
      inputPanel = getInputPanel("inputPanel");
    }
    uploadForm.add(inputPanel);

    final String[] fileUploadLabels = getFileUploadLabels();
    final boolean[] fileUploadRequireds = getFileUploadRequireds();
    final int numberOfFileUploads = fileUploadLabels.length;

    final String fileUploadLabelsText = numberOfFileUploads == 1 ? "Select file:" : "Select files:";
    final Label fileUploadsLabel = new Label("fileUploadsLabel", fileUploadLabelsText);
    uploadForm.add(fileUploadsLabel);

    final List<Integer> fileUploadNumbers = new ArrayList<Integer>(numberOfFileUploads);
    for (int i = 0; i < numberOfFileUploads; i++) {
      fileUploadNumbers.add(i);
    }
    fileUploadFields = new FileUploadField[numberOfFileUploads];
    final ListView<Integer> fileUploadList = new ListView<Integer>("fileUploadList", fileUploadNumbers) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void populateItem(final ListItem<Integer> item) {
        final int numberOfFileUpload = item.getModelObject();
        // Label
        final String fileUploadLabelText = fileUploadLabels[numberOfFileUpload] + ":";
        final Label fileUploadLabel = new Label("fileUploadLabel", fileUploadLabelText);
        fileUploadLabel.setVisible(fileUploadLabelText != null);
        item.add(fileUploadLabel);
        // File upload
        final FileUploadField fileUploadField = new FileUploadField("fileUploadField");
        fileUploadFields[numberOfFileUpload] = fileUploadField;
        item.add(fileUploadField);
      }
    };
    uploadForm.add(fileUploadList);

    feedbackTextArea = new TextArea<String>("feedbackTextArea", feedbackModel);
    feedbackTextArea.setOutputMarkupId(true);
    uploadForm.add(feedbackTextArea);

    final AjaxFallbackButton submitButton =
        new AjaxFallbackButton("submitButton", Model.of(getSubmitButtonLabel()), uploadForm) {
          private static final long serialVersionUID = 1L;

          @Override
          protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
            // Process uploaded files
            final List<String> uploadedFileContents = new ArrayList<String>(numberOfFileUploads);
            for (int i = 0; i < numberOfFileUploads; i++) {
              // Get uploaded file content
              final FileUploadField fileUploadField = fileUploadFields[i];
              final FileUpload uploadedFile = fileUploadField.getFileUpload();
              final boolean fileUploadRequired = fileUploadRequireds[i];
              if (uploadedFile == null && fileUploadRequired) {
                return;
              }
              final String uploadedFileContent;
              if (uploadedFile == null) {
                uploadedFileContent = null;
              } else {
                InputStream uploadStream;
                try {
                  uploadStream = uploadedFile.getInputStream();
                } catch (final IOException e) {
                  LOG.error("Could not read content from uploaded file", e);
                  return;
                }
                final Scanner uploadFileScanner = new Scanner(uploadStream, "UTF-8");
                uploadedFileContent = uploadFileScanner.useDelimiter("\\A").next();
                uploadFileScanner.close();
              }
              // Remember uploaded file content
              uploadedFileContents.add(uploadedFileContent);
            }
            // Process uploaded files
            try {
              processFileContents(inputPanel, uploadedFileContents);
            }
            // In case of error, show feedback
            catch (final FileProcessingException e) {
              feedbackModel.setObject(e.getMessage());
              target.add(feedbackTextArea);
              return;
            }
            // Otherwise, close
            modalContentWindow.close(target);
            // Update tasks immediately
            target.add(((AbstractAuthenticatedPage) getPage()).getTaskExecutionsPanel());
          }
        };
    uploadForm.add(submitButton);
  }

  protected String getSubmitButtonLabel() {
    return "Submit";
  }

  protected Panel getInputPanel(final String id) {
    return new EmptyPanel(id);
  };

  protected abstract void processFileContents(Panel inputPanel, List<String> fileContents)
      throws FileProcessingException;

  protected abstract String[] getFileUploadLabels();

  protected abstract boolean[] getFileUploadRequireds();

}
