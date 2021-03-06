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
package org.immregistries.vfa.manager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.immregistries.vfa.CentralControl;
import org.immregistries.vfa.connect.model.Event;
import org.immregistries.vfa.connect.model.VaccineGroup;
import org.immregistries.vfa.manager.TestCaseImporter;
import org.immregistries.vfa.manager.readers.MiisTestCaseReader;
import org.immregistries.vfa.model.Available;
import org.immregistries.vfa.model.ForecastExpected;
import org.immregistries.vfa.model.TaskGroup;
import org.immregistries.vfa.model.TestCaseWithExpectations;
import org.immregistries.vfa.model.TestPanel;
import org.immregistries.vfa.model.User;
import org.immregistries.vfa.connect.model.TestEvent;

public class TestMiisTestCaseReader extends TestCase{
  public void testConstruct() throws Exception
  {
   
    InputStream in = this.getClass().getResourceAsStream("/IFMTestCasesMaster20120813.csv");
    assertNotNull(in);
    SessionFactory factory = CentralControl.getSessionFactory();
    Session dataSession = factory.openSession();
    User user = (User) dataSession.get(User.class, 1);
    Query query = dataSession.createQuery("from Event");
    List<Event> eventList = query.list();
    query = dataSession.createQuery("from VaccineGroup");
    List<VaccineGroup> vaccineGroupList = query.list();
    Map<Integer, VaccineGroup> vaccineGroupListMap = new HashMap<Integer, VaccineGroup>();
    for (VaccineGroup vaccineGroup : vaccineGroupList)
    {
      vaccineGroupListMap.put(vaccineGroup.getVaccineGroupId(), vaccineGroup);
    }
    MiisTestCaseReader miisTestCaseReader = new MiisTestCaseReader();
    miisTestCaseReader.setEventList(eventList);
    miisTestCaseReader.setUser(user);
    miisTestCaseReader.setVaccineGroupss(vaccineGroupListMap);
    miisTestCaseReader.read(in);
    assertEquals(840, miisTestCaseReader.getTestCaseList().size());
    
    TestCaseWithExpectations testCaseWithExpectations = miisTestCaseReader.getTestCaseList().get(3);
    
    assertNotNull(testCaseWithExpectations);
    assertEquals("1003", testCaseWithExpectations.getTestCase().getTestCaseNumber());
    assertEquals("HepA", testCaseWithExpectations.getTestCase().getCategoryName());
    List<ForecastExpected> forecastExpectedList = testCaseWithExpectations.getForecastExpectedList();
    assertNotNull(forecastExpectedList);
    List<TestEvent> testEventList = testCaseWithExpectations.getTestCase().getTestEventList();
    assertNotNull(testEventList);
    for (TestEvent testEvent : testEventList)
    {
      System.out.println("--> " + testEvent.getEvent().getVaccineCvx() + " " + testEvent.getEventDate());
    }
    assertEquals(1, testEventList.size());
    assertEquals("83", testEventList.get(0).getEvent().getVaccineCvx());
    
    dataSession.close();
    factory.close();
  }
  
  public void testImport() throws Exception
  {
    String filename = "IFMTestCasesMaster20120813";
    InputStream in = this.getClass().getResourceAsStream("/" + filename + ".csv");
    assertNotNull(in);
    SessionFactory factory = CentralControl.getSessionFactory();
    Session dataSession = factory.openSession();
    Transaction transaction = dataSession.beginTransaction();
    
    User user = (User) dataSession.get(User.class, 1);
    Query query = dataSession.createQuery("from Event");
    List<Event> eventList = query.list();
    query = dataSession.createQuery("from VaccineGroup");
    List<VaccineGroup> vaccineGroupList = query.list();
    Map<Integer, VaccineGroup> vaccineGroupListMap = new HashMap<Integer, VaccineGroup>();
    for (VaccineGroup vaccineGroupItem : vaccineGroupList)
    {
      vaccineGroupListMap.put(vaccineGroupItem.getVaccineGroupId(), vaccineGroupItem);
    }
    MiisTestCaseReader miisTestCaseReader = new MiisTestCaseReader();
    miisTestCaseReader.setEventList(eventList);
    miisTestCaseReader.setUser(user);
    miisTestCaseReader.setVaccineGroupss(vaccineGroupListMap);
    miisTestCaseReader.read(in);
    TestCaseImporter tci = new TestCaseImporter();
    
    // find testPanelCase
    TaskGroup taskGroup = (TaskGroup) dataSession.get(TaskGroup.class, 4);
    query = dataSession.createQuery("from TestPanel where taskGroup = ? and label = ?");
    query.setParameter(0, taskGroup);
    query.setParameter(1, filename);
    List<TestPanel> testPanelList = query.list();
    TestPanel testPanel = null;
    if (testPanelList.size() > 0)
    {
      testPanel = testPanelList.get(0);
    }
    else
    {
      testPanel = new TestPanel();
      testPanel.setLabel(filename);
      testPanel.setAvailable(Available.PUBLIC);
      testPanel.setTaskGroup(taskGroup);
      dataSession.save(testPanel);
    }
    tci.importTestCases(miisTestCaseReader, testPanel, dataSession);
    
    transaction.commit();
    dataSession.close();
    factory.close();
    
  }
}
