package edu.illinois.library.cantaloupe.resource;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

/**
 * Representation for Velocity templates. Instances should be obtained from
 * {@link AbstractResource#template}.
 */
public class VelocityRepresentation extends OutputRepresentation {

    static {
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        Velocity.setProperty("class.resource.loader.cache", true);
        Velocity.setProperty("space.gobbling", "structured");
        Velocity.init();
    }

    private String templateName;
    private Map<String,Object> templateVars;

    VelocityRepresentation(String templateName) {
        super(MediaType.TEXT_HTML);
        setCharacterSet(CharacterSet.UTF_8);
        this.templateName = templateName;
    }

    VelocityRepresentation(String templateName,
                           Map<String,Object> templateVars) {
        this(templateName);
        this.templateVars = templateVars;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        VelocityContext context = new VelocityContext();

        if (templateVars != null) {
            for (String key : templateVars.keySet()) {
                context.put(key, templateVars.get(key));
            }
        }

        Template template = Velocity.getTemplate(templateName);

        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
            template.merge(context, writer);
        }
    }

}
