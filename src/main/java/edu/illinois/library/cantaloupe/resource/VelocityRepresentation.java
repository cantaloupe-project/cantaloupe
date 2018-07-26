package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.util.StringUtil;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Representation for Velocity templates.
 */
public class VelocityRepresentation implements Representation {

    private String templateName;
    private Map<String, Object> templateVars;

    static {
        Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        Velocity.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        Velocity.setProperty("class.resource.loader.cache", true);
        // http://velocity.apache.org/engine/2.0/developer-guide.html#space-gobbling
        Velocity.setProperty("space.gobbling", "lines");
        Velocity.init();

    }

    /**
     * @param templateName Template pathname, with leading slash.
     */
    public VelocityRepresentation(String templateName) {
        this.templateName = templateName;
    }

    /**
     * @param templateName Template pathname, with leading slash.
     * @param templateVars Template variables.
     */
    public VelocityRepresentation(String templateName,
                                  Map<String,Object> templateVars) {
        this(templateName);
        this.templateVars = templateVars;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        // Copy template variables into the VelocityContext
        VelocityContext context = new VelocityContext();
        for (Map.Entry<String,Object> entry : templateVars.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

        Template template = Velocity.getTemplate(templateName);

        try (OutputStreamWriter writer =
                     new OutputStreamWriter(outputStream, "UTF-8")) {
            template.merge(context, writer);
        }
    }

}
