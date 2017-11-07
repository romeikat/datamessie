package com.romeikat.datamessie.core.base.ui.page;

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

import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;
import com.romeikat.datamessie.core.base.ui.behavior.FocusBehavior;

public class SignInPage extends AbstractPage {

  public SignInPage(final PageParameters pageParameters) {
    super(pageParameters);
  }

  private static final long serialVersionUID = 1L;

  private String username;

  private String password;

  @Override
  protected void onInitialize() {
    super.onInitialize();

    // Signin page sould be stateless
    setStatelessHint(true);

    // Form
    final StatelessForm<Void> form = new StatelessForm<Void>("signInForm") {
      private static final long serialVersionUID = 1L;

      @Override
      protected void onSubmit() {
        if (Strings.isEmpty(username)) {
          return;
        }
        // Authenticate
        final boolean authResult = AuthenticatedWebSession.get().signIn(username, password);
        // If authentication succeeds, redirect user to the requested page
        if (authResult) {
          continueToOriginalDestination();
          // If we reach this line there was no intercept page, so go to home page
          setResponsePage(getApplication().getHomePage());
        }
      }
    };
    form.setDefaultModel(new CompoundPropertyModel<SignInPage>(this));
    add(form);

    // Username
    final TextField<String> usernameTextField = new TextField<String>("username");
    usernameTextField.add(new FocusBehavior());
    form.add(usernameTextField);
    // Password
    final PasswordTextField passwordTextField = new PasswordTextField("password");
    form.add(passwordTextField);
  }

}
