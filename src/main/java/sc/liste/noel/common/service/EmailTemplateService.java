package sc.liste.noel.common.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Service
public class EmailTemplateService {

    ClassLoaderTemplateResolver resolver;
    TemplateEngine templateEngine;

    public EmailTemplateService() {
        resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    public String generateBodyActivationEmail(String email, String url) {
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("url", url);
        return templateEngine.process("activation-email", context);
    }

    public String generateGiftDeletionBody(String title,
                                           String description,
                                           String url,
                                           String listName,
                                           String shareUrl) {
        Context context = new Context();
        context.setVariable("title", title);
        context.setVariable("description", description);
        context.setVariable("url", url);
        context.setVariable("listName", listName);
        context.setVariable("listUrl", shareUrl);
        return templateEngine.process("suppression-objet", context);
    }

    public String generateGiftModificationBody(String oldTitle,
                                               String oldDescription,
                                               String oldUrl,
                                               String oldPriorityLabel,
                                               String newTitle,
                                               String newDescription,
                                               String newUrl,
                                               String priorityLabel,
                                               String listName,
                                               String listUrl) {
        Context context = new Context();
        context.setVariable("oldTitle", oldTitle);
        context.setVariable("oldDescription", oldDescription);
        context.setVariable("oldUrl", oldUrl);
        context.setVariable("oldPriorityLabel", oldPriorityLabel);

        context.setVariable("newTitle", newTitle);
        context.setVariable("newDescription", newDescription);
        context.setVariable("newUrl", newUrl);
        context.setVariable("priorityLabel", priorityLabel);

        context.setVariable("listName", listName);
        context.setVariable("listUrl", listUrl);
        return templateEngine.process("modification-objet", context);
    }

}
