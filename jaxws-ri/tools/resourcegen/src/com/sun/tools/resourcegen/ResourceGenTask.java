package com.sun.tools.resourcegen;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.text.MessageFormat;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.xml.bind.api.impl.NameConverter;

/**
 * Generate source files from resource bundles.
 *
 * @author Kohsuke Kawaguchi
 */
public class ResourceGenTask extends Task {
    /**
     * Resource files to be compiled
     */
    private FileSet resources;

    private File destDir;

    public void addConfiguredResource( FileSet fs ) {
        resources = fs;
    }

    public void setDestDir(File dir) {
        this.destDir = dir;
    }

    public void execute() throws BuildException {
        if(resources==null)
            throw new BuildException("No resource file is specified");
        if(destDir==null)
            throw new BuildException("No destdir attribute is specified");

        destDir.mkdirs();

        JCodeModel cm = new JCodeModel();

        DirectoryScanner ds = resources.getDirectoryScanner(getProject());
        String[] includedFiles = ds.getIncludedFiles();
        File baseDir = ds.getBasedir();

        for (String value : includedFiles) {
            File res = new File(baseDir, value);

            String className = getClassName(res);

            String bundleName = value.substring(0, value.lastIndexOf('.')).replace('/', '.').replace('\\', '.');// cut off '.properties'
            String dirName = bundleName.substring(0, bundleName.lastIndexOf('.'));

            File destFile = new File(new File(destDir,dirName.replace('.','/')),className+".java");
            if(destFile.lastModified() >= res.lastModified()) {
                log("Skipping "+res,Project.MSG_INFO);
                continue;
            }

            log("Processing "+res,Project.MSG_INFO);
            JPackage pkg = cm._package(dirName);

            Properties props = new Properties();
            try {
                FileInputStream in = new FileInputStream(res);
                props.load(in);
                in.close();
            } catch (IOException e) {
                throw new BuildException(e);
            }

            JDefinedClass clazz;
            try {
                clazz = pkg._class(JMod.PUBLIC | JMod.FINAL, className);
            } catch (JClassAlreadyExistsException e) {
                throw new BuildException("Name conflict "+className);
            }

            clazz.javadoc().add(
                "Defines string formatting method for each constant in the resource file"
            );

            /*
              [RESULT]

                LocalizableMessageFactory messageFactory =
                    new LocalizableMessageFactory("com.sun.xml.ws.resources.client");
                Localizer localizer = new Localizer();
            */

            JClass lmf_class = null;
            JClass l_class   = null;
            JClass lable_class   = null;
            try {
                lmf_class = cm.parseType("com.sun.xml.ws.util.localization.LocalizableMessageFactory").boxify();
                l_class = cm.parseType("com.sun.xml.ws.util.localization.Localizer").boxify();
                lable_class = cm.parseType("com.sun.xml.ws.util.localization.Localizable").boxify();
            } catch (ClassNotFoundException e) {
                throw new BuildException(e); // impossible -- but why parseType throwing ClassNotFoundExceptoin!?
            }

            JFieldVar $msgFactory = clazz.field(JMod.PRIVATE|JMod.STATIC|JMod.FINAL,
                lmf_class, "messageFactory", JExpr._new(lmf_class).arg(JExpr.lit(bundleName)));

            JFieldVar $localizer = clazz.field(JMod.PRIVATE|JMod.STATIC|JMod.FINAL,
                l_class, "localizer", JExpr._new(l_class));

            for (Object key : props.keySet()) {
                // [RESULT]
                // Localizable METHOD_localizable(Object arg1, Object arg2, ...) {
                //   return messageFactory.getMessage("servlet.html.notFound", message));
                // }
                // String METHOD(Object arg1, Object arg2, ...) {
                //   return localizer.localize(METHOD_localizable(arg1,arg2,...));
                // }
                String methodBaseName = NameConverter.smart.toConstantName(key.toString());

                JMethod method = clazz.method(JMod.PUBLIC | JMod.STATIC, lable_class, methodBaseName+"_localizable");

                int countArgs = countArgs(props.getProperty(key.toString()));

                JInvocation format = $msgFactory.invoke("getMessage").arg(
                    JExpr.lit(key.toString()));

                for( int i=0; i<countArgs; i++ ) {
                    format.arg( method.param(Object.class,"arg"+i));
                }
                method.body()._return(format);

                JMethod method2 = clazz.method(JMod.PUBLIC|JMod.STATIC, String.class, methodBaseName);
                method2.javadoc().add(props.get(key));

                JInvocation localize = JExpr.invoke(method);
                for( int i=0; i<countArgs; i++ ) {
                    localize.arg( method2.param(Object.class,"arg"+i));
                }

                method2.body()._return($localizer.invoke("localize").arg(localize));
            }
        }

        try {
            cm.build(destDir);
        } catch (IOException e) {
            throw new BuildException("Failed to generate code",e);
        }
    }

    /**
     * Counts the number of arguments.
     */
    private int countArgs(String value) {
        List<String> x = new ArrayList<String>();

        while(true) {
            String r1 = MessageFormat.format(value, x.toArray());
            x.add("xxxx");
            String r2 = MessageFormat.format(value, x.toArray());

            if(r1.equals(r2))
                return x.size()-1;
        }
    }

    private String getClassName(File res) {
        String name = res.getName();
        int suffixIndex = name.lastIndexOf('.');
        name = name.substring(0,suffixIndex);
        return NameConverter.smart.toClassName(name)+"Messages";
    }
}
