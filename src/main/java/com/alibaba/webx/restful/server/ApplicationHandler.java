package com.alibaba.webx.restful.server;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.alibaba.webx.restful.message.MessageBodyWorkers;
import com.alibaba.webx.restful.model.Resource;
import com.alibaba.webx.restful.model.ResourceModelIssue;
import com.alibaba.webx.restful.spi.ContextResolvers;
import com.alibaba.webx.restful.spi.ExceptionMappers;
import com.alibaba.webx.restful.util.Ref;

public class ApplicationHandler {

    private final static Log        LOG = LogFactory.getLog(ApplicationHandler.class);

    private final ApplicationConfig configuration;

    private ApplicationContext      applicationContext;

    private References refs;
    
    public ApplicationHandler(Application application){
        this.configuration = ApplicationConfig.forApplication(application);

        initialize();
    }

    private void initialize() {
        configuration.lock();

        final List<ResourceModelIssue> resourceModelIssues = new LinkedList<ResourceModelIssue>();

        final Map<String, Resource.Builder> pathToResourceBuilderMap = new HashMap<String, Resource.Builder>();
        final List<Resource.Builder> resourcesBuilders = new LinkedList<Resource.Builder>();
        for (Class<?> c : configuration.getClasses()) {
            Path path = Resource.getPath(c);
            if (path != null) { // root resource
                try {
                    final Resource.Builder builder = Resource.builder(c, resourceModelIssues);
                    resourcesBuilders.add(builder);
                    pathToResourceBuilderMap.put(path.value(), builder);
                } catch (IllegalArgumentException ex) {
                    LOG.warn(ex.getMessage());
                }
            }
        }

        for (Resource programmaticResource : configuration.getResources()) {
            Resource.Builder builder = pathToResourceBuilderMap.get(programmaticResource.getPath());
            if (builder != null) {
                builder.mergeWith(programmaticResource);
            } else {
                resourcesBuilders.add(Resource.builder(programmaticResource));
            }
        }

        this.refs = (References) applicationContext.getAutowireCapableBeanFactory().createBean(References.class);
    }

    private static class References {

        @Autowired
        private Ref<ExceptionMappers>   mappers;
        @Autowired
        private Ref<MessageBodyWorkers> workers;
        @Autowired
        private Ref<ContextResolvers>   resolvers;
    }
}
