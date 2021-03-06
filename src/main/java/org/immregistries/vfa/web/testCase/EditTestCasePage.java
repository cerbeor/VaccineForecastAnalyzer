/*
 * Copyright 2013 - Texas Children's Hospital
 * 
 *   Texas Children's Hospital licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package org.immregistries.vfa.web.testCase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.yui.calendar.DatePicker;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.StringValidator;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.immregistries.vfa.connect.model.Condition;
import org.immregistries.vfa.connect.model.Event;
import org.immregistries.vfa.connect.model.EventType;
import org.immregistries.vfa.connect.model.ServiceOption;
import org.immregistries.vfa.connect.model.Software;
import org.immregistries.vfa.connect.model.TestCase;
import org.immregistries.vfa.connect.model.TestCaseSetting;
import org.immregistries.vfa.connect.model.TestEvent;
import org.immregistries.vfa.model.Include;
import org.immregistries.vfa.model.Result;
import org.immregistries.vfa.model.TestPanelCase;
import org.immregistries.vfa.model.User;
import org.immregistries.vfa.web.FTBasePage;
import org.immregistries.vfa.web.MenuSection;
import org.immregistries.vfa.web.SecurePage;
import org.immregistries.vfa.web.WebSession;

public class EditTestCasePage extends FTBasePage implements SecurePage {
  private static final long serialVersionUID = 1L;
  
  private Model<String> categoryName;
  private Model<Include> includeStatus;
  private Model<Result> resultStatus;

  public EditTestCasePage() {
    this(new PageParameters());
  }

  public EditTestCasePage(final PageParameters pageParameters) {
    super(MenuSection.TEST_CASE, pageParameters);
    final WebSession webSession = ((WebSession) getSession());
    final User user = webSession.getUser();
    final TestCase testCase = user.getSelectedTestCase();
    final Session dataSession = webSession.getDataSession();

    add(new FeedbackPanel("feedback"));

    Form<TestCase> testCaseForm = new Form<TestCase>("testCaseForm", new CompoundPropertyModel<TestCase>(testCase)) {
      @Override
      protected void onSubmit() {
        Transaction transaction = dataSession.beginTransaction();
        dataSession.update(testCase);
        transaction.commit();
      }
    };

    add(testCaseForm);

    TextField<String> labelField = new TextField<String>("label");
    labelField.add(StringValidator.maximumLength(120));
    labelField.setRequired(true);
    testCaseForm.add(labelField);

    TextArea<String> descriptionField = new TextArea<String>("description");
    descriptionField.add(StringValidator.maximumLength(4000));
    descriptionField.setRequired(true);
    testCaseForm.add(descriptionField);

    TextField<String> patientFirstField = new TextField<String>("patientFirst");
    patientFirstField.add(StringValidator.maximumLength(30));
    patientFirstField.setRequired(true);
    testCaseForm.add(patientFirstField);

    TextField<String> patientLastField = new TextField<String>("patientLast");
    patientLastField.add(StringValidator.maximumLength(30));
    patientLastField.setRequired(true);
    testCaseForm.add(patientLastField);

    List<String> patientSexList = new ArrayList<String>();
    patientSexList.add(TestCase.PATIENT_SEX_FEMALE);
    patientSexList.add(TestCase.PATIENT_SEX_MALE);
    DropDownChoice<String> patientSexField = new DropDownChoice<String>("patientSex", patientSexList);
    patientSexField.setRequired(true);
    testCaseForm.add(patientSexField);

    TextField<Date> patientDobField = new TextField<Date>("patientDob");
    patientDobField.setRequired(true);
    patientDobField.add(new DatePicker());
    testCaseForm.add(patientDobField);

    TextField<Date> evalDateField = new TextField<Date>("evalDate");
    evalDateField.setRequired(true);
    evalDateField.add(new DatePicker());
    testCaseForm.add(evalDateField);

    TestPanelCase selectedTestPanelCase = user.getSelectedTestPanelCase();

    final TestPanelCase testPanelCase = selectedTestPanelCase == null ? new TestPanelCase() : user
        .getSelectedTestPanelCase();
    Form<TestPanelCase> editTestPanelCaseForm = new Form<TestPanelCase>("editTestPanelCaseForm",
        new CompoundPropertyModel<TestPanelCase>(testPanelCase)) {
      @Override
      protected void onSubmit() {
        if (testPanelCase.getTestCaseNumber() != null && testPanelCase.getTestCaseNumber().length() > 0)
        {
          // make sure it's unique for test panel
          Query query = dataSession.createQuery("from TestPanelCase where testPanel = ? and testCaseNumber = ?");
          query.setParameter(0, testPanelCase.getTestPanel());
          query.setParameter(1, testPanelCase.getTestCaseNumber());
          for (TestPanelCase tpc : (List<TestPanelCase>) query.list())
          {
            if (tpc.equals(testPanelCase))
            {
              continue;
            }
            error("Test case number has already been used by another test case in this test panel");
            return;
          }
        }
        Transaction transaction = dataSession.beginTransaction();
        dataSession.saveOrUpdate(testPanelCase);
        user.setSelectedTestPanelCase(testPanelCase);
        transaction.commit();
      }
    };

    add(editTestPanelCaseForm);
    TextField<String> categoryNameField = new TextField<String>("categoryName");
    categoryNameField.setRequired(true);
    categoryNameField.add(StringValidator.maximumLength(120));
    editTestPanelCaseForm.add(categoryNameField);

    TextField<String> testCaseNumberField = new TextField<String>("testCaseNumber");
    testCaseNumberField.add(StringValidator.maximumLength(120));
    editTestPanelCaseForm.add(testCaseNumberField);
    
    DropDownChoice<Include> includeStatusField = new DropDownChoice<Include>("include", Include.valueList());
    includeStatusField.setRequired(true);
    editTestPanelCaseForm.add(includeStatusField);

    DropDownChoice<Result> resultStatusField = new DropDownChoice<Result>("result", Result.valueList());
    editTestPanelCaseForm.add(resultStatusField);

    if (selectedTestPanelCase == null) {
      testPanelCase.setTestPanel(user.getSelectedTestPanel());
      testPanelCase.setTestCase(testCase);
    }
    if (user.getSelectedTestPanel() == null) {
      editTestPanelCaseForm.setVisible(false);
    }

    Query query = webSession.getDataSession().createQuery("from TestEvent where testCase = ?");
    query.setParameter(0, testCase);
    List<TestEvent> testEventList = query.list();

    ListView<TestEvent> testEventItems = new ListView<TestEvent>("testEventItems", testEventList) {

      protected void populateItem(ListItem<TestEvent> item) {
        final TestEvent testEvent = item.getModelObject();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        item.add(new Label("eventType", testEvent.getEvent().getEventType().getLabel()));
        item.add(new Label("label", testEvent.getEvent().getLabel()));
        item.add(new Label("vacineCvx", testEvent.getEvent().getVaccineCvx()));
        item.add(new Label("vacineMvx", testEvent.getEvent().getVaccineMvx()));
        item.add(new Label("condition", testEvent.getCondition() == null ? "" : testEvent.getCondition().getLabel()));
        item.add(new Label("eventDate", sdf.format(testEvent.getEventDate())));

        Form<Void> deleteForm = new Form<Void>("deleteForm") {
          @Override
          protected void onSubmit() {
            Transaction transaction = dataSession.beginTransaction();
            dataSession.delete(testEvent);
            transaction.commit();
            setResponsePage(new EditTestCasePage());
          }
        };
        item.add(deleteForm);
      }
    };
    add(testEventItems);

    final TestEvent testEvent = new TestEvent();
    testEvent.setTestCase(testCase);
    Form<TestEvent> addTestEventForm = new Form<TestEvent>("addTestEventForm", new CompoundPropertyModel<TestEvent>(
        testEvent)) {
      @Override
      protected void onSubmit() {
        Transaction transaction = dataSession.beginTransaction();
        dataSession.save(testEvent);
        transaction.commit();
        setResponsePage(new EditTestCasePage());
      }
    };
    add(addTestEventForm);

    query = dataSession.createQuery("from Event where eventTypeCode <> ? and eventTypeCode <> ? order by eventTypeCode, label");
    query.setParameter(0, EventType.ACIP_DEFINED_CONDITION.getEventTypeCode());
    query.setParameter(1, EventType.CONDITION_IMPLICATION.getEventTypeCode());
    List<Event> eventList = query.list();
    DropDownChoice<Event> eventField = new DropDownChoice<Event>("event", eventList);
    eventField.setRequired(true);
    addTestEventForm.add(eventField);
    
    List<Condition> conditionList = Condition.valueList();
    DropDownChoice<Condition> conditionField = new DropDownChoice<Condition>("condition", conditionList);
    addTestEventForm.add(conditionField);

    TextField<Date> eventDate = new TextField<Date>("eventDate");
    eventDate.setRequired(true);
    eventDate.add(new DatePicker());
    addTestEventForm.add(eventDate);
    
    
    
    query = dataSession.createQuery("from TestCaseSetting where testCase = ? order by serviceOption.optionLabel");
    query.setParameter(0, testCase);
    List<TestCaseSetting> testCaseSettingList = query.list();

    ListView<TestCaseSetting> testCaseSettingItems = new ListView<TestCaseSetting>("testCaseSettingItems",
        testCaseSettingList) {

      @Override
      protected void populateItem(ListItem<TestCaseSetting> item) {
        final TestCaseSetting testCaseSetting = item.getModelObject();

        Form<TestCaseSetting> editTestCaseSettingForm = new Form<TestCaseSetting>("editTestCaseSettingForm",
            new CompoundPropertyModel<TestCaseSetting>(testCaseSetting)) {
          @Override
          protected void onSubmit() {
            Transaction transaction = dataSession.beginTransaction();
            dataSession.update(testCaseSetting);
            transaction.commit();
            setResponsePage(new EditTestCasePage());
          }
        };

        TextField<String> optionValue = new TextField<String>("optionValue");
        optionValue.setRequired(true);
        editTestCaseSettingForm.add(optionValue);
        editTestCaseSettingForm.add(new Label("optionLabel", testCaseSetting.getServiceOption().getOptionLabel()));
        editTestCaseSettingForm.add(new Label("description", testCaseSetting.getServiceOption().getDescription()));
        item.add(editTestCaseSettingForm);
      }
    };
    add(testCaseSettingItems);

    // Add form
    final TestCaseSetting testCaseSetting = new TestCaseSetting();
    testCaseSetting.setTestCase(testCase);

    Form<TestCaseSetting> addTestCaseSettingForm = new Form<TestCaseSetting>("addTestCaseSettingForm",
        new CompoundPropertyModel<TestCaseSetting>(testCaseSetting)) {
      @Override
      protected void onSubmit() {
        Transaction transaction = dataSession.beginTransaction();
        dataSession.save(testCaseSetting);
        transaction.commit();
        setResponsePage(new EditTestCasePage());
      }
    };

    TextField<String> optionValue = new TextField<String>("optionValue");
    optionValue.setRequired(true);
    addTestCaseSettingForm.add(optionValue);

    List<ServiceOption> serviceOptionList = getServiceOptionList(dataSession, user.getSelectedTaskGroup().getPrimarySoftware(), testCaseSettingList);
    DropDownChoice<ServiceOption> serviceOption = new DropDownChoice<ServiceOption>("serviceOption", serviceOptionList);
    serviceOption.setRequired(true);
    addTestCaseSettingForm.add(serviceOption);
    
    add(addTestCaseSettingForm);

  }

  public List<ServiceOption> getServiceOptionList(final Session dataSession, final Software software,
      List<TestCaseSetting> testCaseSettingList) {

    Query query = dataSession.createQuery("from ServiceOption where serviceType = ?");
    query.setParameter(0, software.getServiceType());
    List<ServiceOption> serviceOptionList = query.list();
    for (Iterator<ServiceOption> it = serviceOptionList.iterator(); it.hasNext();) {
      ServiceOption serviceOption = it.next();
      for (TestCaseSetting testCaseSettingCompare : testCaseSettingList) {
        if (testCaseSettingCompare.getServiceOption().equals(serviceOption)) {
          it.remove();
          break;
        }
      }
    }
    return serviceOptionList;
  }

}
