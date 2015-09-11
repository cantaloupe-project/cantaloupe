package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.processor.UnsupportedSourceFormatException;
import edu.illinois.library.cantaloupe.resolver.Resolver;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.restlet.data.MediaType;
import org.restlet.ext.velocity.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles the landing page.
 */
public class LandingResource extends AbstractResource {

    @Get
    public Representation doGet() throws Exception {
        Template template = Velocity.getTemplate("landing.vm");
        return new TemplateRepresentation(template, getTemplateVars(),
                MediaType.TEXT_HTML);
    }

    private Map<String,Object> getTemplateVars() throws Exception {
        Map<String,Object> vars = new HashMap<String,Object>();

        // resolver name
        Resolver resolver = ResolverFactory.getResolver();
        String resolverStr = "None";
        if (resolver != null) {
            resolverStr = resolver.getClass().getSimpleName();
        }
        vars.put("resolverName", resolverStr);

        // source formats
        Map<SourceFormat,String> sourceFormats =
                new HashMap<SourceFormat, String>();
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            try {
                sourceFormats.put(sourceFormat,
                        ProcessorFactory.getProcessor(sourceFormat).getClass().getSimpleName());
            } catch (UnsupportedSourceFormatException e) {
                // noop
            }
        }
        vars.put("sourceFormats", sourceFormats);

        // processors
        Map<String,Set<SourceFormat>> processors =
                new HashMap<String, Set<SourceFormat>>();
        for (Processor processor : ProcessorFactory.getAllProcessors()) {
            processors.put(processor.getClass().getSimpleName(),
                    processor.getSupportedSourceFormats());
        }
        vars.put("processors", processors);

        return vars;
    }

}
