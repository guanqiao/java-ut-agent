package com.utagent.coverage;

import com.utagent.model.CoverageInfo;
import com.utagent.model.CoverageReport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JacocoXmlParser {

    public CoverageReport parse(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlFile);
        
        Element root = document.getDocumentElement();
        
        CounterInfo overallCounters = parseCounters(root);
        
        List<CoverageInfo> classCoverages = new ArrayList<>();
        List<Integer> uncoveredLines = new ArrayList<>();
        
        NodeList packages = root.getElementsByTagName("package");
        for (int i = 0; i < packages.getLength(); i++) {
            Element pkg = (Element) packages.item(i);
            String packageName = pkg.getAttribute("name").replace('/', '.');
            
            NodeList classes = pkg.getElementsByTagName("class");
            for (int j = 0; j < classes.getLength(); j++) {
                Element cls = (Element) classes.item(j);
                String className = packageName + "." + cls.getAttribute("name");
                
                CounterInfo classCounters = parseCounters(cls);
                
                CoverageInfo classInfo = new CoverageInfo(
                    className,
                    "",
                    0,
                    classCounters.branchTotal,
                    classCounters.branchMissed,
                    classCounters.instructionTotal,
                    classCounters.instructionMissed,
                    classCounters.lineTotal,
                    classCounters.lineMissed
                );
                classCoverages.add(classInfo);
                
                NodeList methods = pkg.getElementsByTagName("method");
                for (int k = 0; k < methods.getLength(); k++) {
                    Element method = (Element) methods.item(k);
                    String methodName = method.getAttribute("name");
                    
                    if (method.getParentNode().equals(cls)) {
                        CounterInfo methodCounters = parseCounters(method);
                        
                        CoverageInfo methodInfo = new CoverageInfo(
                            className,
                            methodName,
                            0,
                            methodCounters.branchTotal,
                            methodCounters.branchMissed,
                            methodCounters.instructionTotal,
                            methodCounters.instructionMissed,
                            methodCounters.lineTotal,
                            methodCounters.lineMissed
                        );
                        classCoverages.add(methodInfo);
                    }
                }
                
                NodeList sourceFiles = pkg.getElementsByTagName("sourcefile");
                for (int k = 0; k < sourceFiles.getLength(); k++) {
                    Element sourceFile = (Element) sourceFiles.item(k);
                    NodeList lines = sourceFile.getElementsByTagName("line");
                    for (int l = 0; l < lines.getLength(); l++) {
                        Element line = (Element) lines.item(l);
                        int ci = Integer.parseInt(line.getAttribute("ci"));
                        int mi = Integer.parseInt(line.getAttribute("mi"));
                        if (mi > 0 && ci == 0) {
                            uncoveredLines.add(Integer.parseInt(line.getAttribute("nr")));
                        }
                    }
                }
            }
        }
        
        return new CoverageReport(
            overallCounters.getLineCoverageRate(),
            overallCounters.getBranchCoverageRate(),
            overallCounters.getInstructionCoverageRate(),
            classCoverages,
            uncoveredLines
        );
    }
    
    private CounterInfo parseCounters(Element element) {
        CounterInfo info = new CounterInfo();
        
        NodeList counters = element.getElementsByTagName("counter");
        for (int i = 0; i < counters.getLength(); i++) {
            Element counter = (Element) counters.item(i);
            String type = counter.getAttribute("type");
            int missed = Integer.parseInt(counter.getAttribute("missed"));
            int covered = Integer.parseInt(counter.getAttribute("covered"));
            
            switch (type) {
                case "LINE":
                    info.lineMissed = missed;
                    info.lineTotal = missed + covered;
                    break;
                case "BRANCH":
                    info.branchMissed = missed;
                    info.branchTotal = missed + covered;
                    break;
                case "INSTRUCTION":
                    info.instructionMissed = missed;
                    info.instructionTotal = missed + covered;
                    break;
                case "METHOD":
                    info.methodMissed = missed;
                    info.methodTotal = missed + covered;
                    break;
                case "CLASS":
                    info.classMissed = missed;
                    info.classTotal = missed + covered;
                    break;
            }
        }
        
        return info;
    }
    
    private static class CounterInfo {
        int lineMissed = 0;
        int lineTotal = 0;
        int branchMissed = 0;
        int branchTotal = 0;
        int instructionMissed = 0;
        int instructionTotal = 0;
        int methodMissed = 0;
        int methodTotal = 0;
        int classMissed = 0;
        int classTotal = 0;
        
        double getLineCoverageRate() {
            return lineTotal > 0 ? (double)(lineTotal - lineMissed) / lineTotal : 1.0;
        }
        
        double getBranchCoverageRate() {
            return branchTotal > 0 ? (double)(branchTotal - branchMissed) / branchTotal : 1.0;
        }
        
        double getInstructionCoverageRate() {
            return instructionTotal > 0 ? (double)(instructionTotal - instructionMissed) / instructionTotal : 1.0;
        }
    }
}
