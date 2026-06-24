package edu.illinois.library.cantaloupe.resource;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

/**
 * Representation for Thymeleaf HTML templates.
 */
public class ThymeleafRepresentation implements Representation {

    private static final TemplateEngine templateEngine;
    private String templateName;
    private TemplateVariables templateVars;

    static {
        // HTML template resolver
        ClassLoaderTemplateResolver htmlResolver = new ClassLoaderTemplateResolver();
        htmlResolver.setTemplateMode(TemplateMode.HTML);
        htmlResolver.setPrefix("/");
        htmlResolver.setSuffix(".html");
        htmlResolver.setCacheable(true);
        htmlResolver.setCharacterEncoding("UTF-8");

        templateEngine = new TemplateEngine();
        templateEngine.addTemplateResolver(htmlResolver);
    }

    /**
     * @param templateName Template pathname, with leading slash.
     */
    public ThymeleafRepresentation(String templateName) {
        this.templateName = templateName;
    }

    /**
     * @param templateName Template pathname, with leading slash.
     * @param templateVars Template variables.
     */
    public ThymeleafRepresentation(String templateName,
                                   TemplateVariables templateVars) {
        this(templateName);
        this.templateVars = templateVars;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        Context context = new Context();
        if (templateVars != null) {
            for (Map.Entry<String, Object> entry : templateVars.getVars().entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
        }

        // Keep the original template name with extension for proper resolver matching
        String thymeleafTemplateName = templateName;
        if (thymeleafTemplateName.startsWith("/")) {
            thymeleafTemplateName = thymeleafTemplateName.substring(1);
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8")) {
            templateEngine.process(thymeleafTemplateName, context, writer);
        }
    }
}
