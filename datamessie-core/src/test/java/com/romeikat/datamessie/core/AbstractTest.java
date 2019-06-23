package com.romeikat.datamessie.core;

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

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import com.romeikat.datamessie.core.base.util.SpringUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration("/applicationContext.xml")
public abstract class AbstractTest {

  protected final static double ALLOWED_DELTA = 0.001d;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void after() throws Exception {}

  protected <T> T createMock(final Class<T> classToBeMocked) {
    // Create mock
    final T mock = Mockito.mock(classToBeMocked);
    // Done
    return mock;
  }

  protected <T> T createAndInjectMock(final Class<T> classToBeMocked,
      final Object objectContainingMocked, final String fieldNameContainingMock) throws Exception {
    // Create spy
    final T mock = createMock(classToBeMocked);
    // Inject spy
    setField(objectContainingMocked, fieldNameContainingMock, mock);
    // Done
    return mock;
  }

  protected <T> T createSpy(final T objectToBeSpied) throws Exception {
    // Unproxy object to be spied, if necessary
    final T objectToBeSpiedUnproxied = SpringUtil.unwrapProxy(objectToBeSpied);
    // Create spy
    final T spy = Mockito.spy(objectToBeSpiedUnproxied);
    // Done
    return spy;
  }

  protected <T> T createAndInjectSpy(final T objectToBeSpied, final Object objectContainingSpy,
      final String fieldNameContainingSpy) throws Exception {
    // Create spy
    final T spy = createSpy(objectToBeSpied);
    // Inject spy
    setField(objectContainingSpy, fieldNameContainingSpy, spy);
    // Done
    return spy;
  }

  protected <T> void setField(final Object targetObject, final String name, final T value) {
    // Inject value
    ReflectionTestUtils.setField(targetObject, name, value);
  }

}
