/*
 * Copyright 2014 Bernd Vogt and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sourcepit.tpmp;

import java.io.File;

import org.sourcepit.common.utils.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

public class TargetPlatformWriter {
   public void write(File targetFile, String name, String location, TargetEnvironment targetEnvironment,
      String executionEnvironment) {
      final Document doc = XmlUtils.newDocument();

      ProcessingInstruction pdeVersion = doc.createProcessingInstruction("pde", "version=\"3.6\"");
      doc.appendChild(pdeVersion);

      Element targetElem = doc.createElement("target");
      targetElem.setAttribute("name", name);
      doc.appendChild(targetElem);

      if (targetEnvironment != null) {
         appendTargetEnvironment(targetElem, targetEnvironment);
      }
      if (executionEnvironment != null) {
         appendExecutionEnvironment(targetElem, executionEnvironment);
      }

      Element locationsElem = doc.createElement("locations");
      targetElem.appendChild(locationsElem);

      Element locationElem = doc.createElement("location");
      locationElem.setAttribute("path", location);
      locationElem.setAttribute("type", "Profile");
      locationsElem.appendChild(locationElem);

      XmlUtils.writeXml(doc, targetFile);
   }

   private void appendTargetEnvironment(Element parentElem, TargetEnvironment targetEnvironment) {
      Document doc = parentElem.getOwnerDocument();

      Element environmentElem = doc.createElement("environment");
      parentElem.appendChild(environmentElem);

      Element osElem = doc.createElement("os");
      osElem.setTextContent(targetEnvironment.getOs());
      environmentElem.appendChild(osElem);

      Element wsElem = doc.createElement("ws");
      wsElem.setTextContent(targetEnvironment.getWs());
      environmentElem.appendChild(wsElem);

      Element archElem = doc.createElement("arch");
      archElem.setTextContent(targetEnvironment.getArch());
      environmentElem.appendChild(archElem);
   }

   private void appendExecutionEnvironment(Element parentElem, String executionEnvironment) {
      Element jreElem = parentElem.getOwnerDocument().createElement("targetJRE");
      jreElem.setAttribute("path",
         "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/"
            + executionEnvironment);
      parentElem.appendChild(jreElem);
   }
}
