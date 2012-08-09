/**
 * Copyright (c) 2012 Sourcepit.org contributors and others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.sourcepit.mtp;

import java.io.File;

import org.sourcepit.common.utils.xml.XmlUtils;
import org.sourcepit.mtp.te.TargetEnvironment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

public class TargetPlatformWriter
{
   public void write(File targetFile, String name, File location, TargetEnvironment targetEnvironment,
      String executionEnvironment)
   {
      final Document doc = XmlUtils.newDocument();

      ProcessingInstruction pdeVersion = doc.createProcessingInstruction("pde", "version=\"3.6\"");
      doc.appendChild(pdeVersion);

      Element targetElem = doc.createElement("target");
      targetElem.setAttribute("name", name);
      doc.appendChild(targetElem);

      if (targetEnvironment != null)
      {
         appendTargetEnvironment(targetElem, targetEnvironment);
      }
      if (executionEnvironment != null)
      {
         appendExecutionEnvironment(targetElem, executionEnvironment);
      }

      Element locationsElem = doc.createElement("locations");
      targetElem.appendChild(locationsElem);

      Element locationElem = doc.createElement("location");
      locationElem.setAttribute("path", location.getAbsolutePath());
      locationElem.setAttribute("type", "Profile");
      locationsElem.appendChild(locationElem);

      XmlUtils.writeXml(doc, targetFile);
   }

   private void appendTargetEnvironment(Element parentElem, TargetEnvironment targetEnvironment)
   {
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

      if (targetEnvironment.getNl() != null)
      {
         Element nlElem = doc.createElement("nl");
         nlElem.setTextContent(targetEnvironment.getNl());
         environmentElem.appendChild(nlElem);
      }
   }

   private void appendExecutionEnvironment(Element parentElem, String executionEnvironment)
   {
      Element jreElem = parentElem.getOwnerDocument().createElement("targetJRE");
      jreElem.setAttribute("path",
         "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/"
            + executionEnvironment);
      parentElem.appendChild(jreElem);
   }
}
