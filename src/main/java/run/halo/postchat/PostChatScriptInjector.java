package run.halo.postchat;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.pf4j.PluginWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.theme.dialect.TemplateHeadProcessor;
import run.halo.app.plugin.ReactiveSettingFetcher;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostChatScriptInjector implements TemplateHeadProcessor {

    static final PropertyPlaceholderHelper PROPERTY_PLACEHOLDER_HELPER = new PropertyPlaceholderHelper("${", "}");

    private final PluginWrapper pluginWrapper;
    private final ReactiveSettingFetcher reactiveSettingFetcher;

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
                              IElementModelStructureHandler structureHandler) {
        final IModelFactory modelFactory = context.getModelFactory();

        return Mono.zip(
            fetchSetting("chat", PostChatConfig.class),
            fetchSetting("account", AccountConfig.class),
            fetchSetting("summary", SummaryConfig.class)
        ).flatMap(tuple -> {
            PostChatConfig postChatConfig = tuple.getT1().orElse(new PostChatConfig());
            AccountConfig accountConfig = tuple.getT2().orElse(new AccountConfig());
            SummaryConfig summaryConfig = tuple.getT3().orElse(new SummaryConfig());
            
            model.add(modelFactory.createText(postChatScript(postChatConfig, accountConfig, summaryConfig)));
            return Mono.empty();
        }).then()
        .subscribeOn(Schedulers.boundedElastic());
    }

    private <T> Mono<java.util.Optional<T>> fetchSetting(String group, Class<T> clazz) {
        return reactiveSettingFetcher.fetch(group, clazz)
            .map(Optional::ofNullable)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Data
    public static class PostChatConfig {
        public static final String GROUP = "chat";
        private boolean enableAI;
        private String backgroundColor;
        private String bottom;
        private String left;
        private String fill;
        private String width;
        private String frameWidth;
        private String frameHeight;
        private boolean defaultInput;
        private boolean upLoadWeb;
        private boolean showInviteLink;
        private String userTitle;
        private String userDesc;
        private boolean addButton;
        private String userMode = "magic";
        private String userIcon = "";

        @Data
        public static class ChatQuestion {
            private String question;
        }

        private List<ChatQuestion> defaultChatQuestions = new ArrayList<>();
        private List<ChatQuestion> defaultSearchQuestions = new ArrayList<>();
    }

    @Data
    public static class AccountConfig {
        public static final String GROUP = "account";
        private String key;
    }

    @Data
    public static class SummaryConfig {
        public static final String GROUP = "summary";
        private boolean enableSummary;
        private String postSelector;
        private String title;
        private String summaryStyle;
        private String postURL;
        private String blacklist;
        private String wordLimit;
        private boolean typingAnimate;
        private String beginningText;
        private String summaryTheme;
    }

    private String postChatScript(PostChatConfig postChatConfig, AccountConfig accountConfig, SummaryConfig summaryConfig) {
        final Properties properties = new Properties();
        properties.setProperty("enableAI", String.valueOf(postChatConfig.isEnableAI()));
        properties.setProperty("backgroundColor", postChatConfig.getBackgroundColor());
        properties.setProperty("bottom", postChatConfig.getBottom());
        properties.setProperty("left", postChatConfig.getLeft());
        properties.setProperty("fill", postChatConfig.getFill());
        properties.setProperty("width", postChatConfig.getWidth());
        properties.setProperty("frameWidth", postChatConfig.getFrameWidth());
        properties.setProperty("frameHeight", postChatConfig.getFrameHeight());
        properties.setProperty("defaultInput", String.valueOf(postChatConfig.isDefaultInput()));
        properties.setProperty("upLoadWeb", String.valueOf(postChatConfig.isUpLoadWeb()));
        properties.setProperty("showInviteLink", String.valueOf(postChatConfig.isShowInviteLink()));
        properties.setProperty("userTitle", String.valueOf(postChatConfig.getUserTitle()));
        properties.setProperty("userDesc", String.valueOf(postChatConfig.getUserDesc()));
        properties.setProperty("addButton", String.valueOf(postChatConfig.isAddButton()));
        properties.setProperty("userMode", postChatConfig.getUserMode());
        properties.setProperty("userIcon", postChatConfig.getUserIcon());

        // 修改数组格式化方式，添加空值检查
        String defaultChatQuestionsStr = postChatConfig.getDefaultChatQuestions().stream()
                .filter(q -> q != null && q.getQuestion() != null)
                .map(q -> "\"" + q.getQuestion() + "\"")
                .collect(Collectors.joining(","));
        
        String defaultSearchQuestionsStr = postChatConfig.getDefaultSearchQuestions().stream()
                .filter(q -> q != null && q.getQuestion() != null)
                .map(q -> "\"" + q.getQuestion() + "\"")
                .collect(Collectors.joining(","));

        properties.setProperty("defaultChatQuestions", defaultChatQuestionsStr);
        properties.setProperty("defaultSearchQuestions", defaultSearchQuestionsStr);

        // 账户设置
        properties.setProperty("account_key", String.valueOf(accountConfig.getKey()));

        // 文章摘要设置
        properties.setProperty("enableSummary", String.valueOf(summaryConfig.isEnableSummary()));
        properties.setProperty("summary_postSelector", String.valueOf(summaryConfig.getPostSelector()));
        properties.setProperty("summary_title", String.valueOf(summaryConfig.getTitle()));
        properties.setProperty("summary_style", String.valueOf(summaryConfig.getSummaryStyle()));
        properties.setProperty("summary_postURL", String.valueOf(summaryConfig.getPostURL()));
        properties.setProperty("summary_blacklist", String.valueOf(summaryConfig.getBlacklist()));
        properties.setProperty("summary_wordLimit", String.valueOf(summaryConfig.getWordLimit()));
        properties.setProperty("summary_typingAnimate", String.valueOf(summaryConfig.isTypingAnimate()));
        properties.setProperty("summary_beginningText", String.valueOf(summaryConfig.getBeginningText()));
        properties.setProperty("summary_theme", String.valueOf(summaryConfig.getSummaryTheme()));

        String scriptUrl = "";
        String cssLink = "";

        if (postChatConfig.isEnableAI() && summaryConfig.isEnableSummary()) {
            scriptUrl = "https://ai.tianli0.top/static/public/postChatUser_summary.min.js";
            cssLink = "<link rel=\"stylesheet\" href=\"${summary_style}\">";
        } else if (postChatConfig.isEnableAI()) {
            scriptUrl = "https://ai.tianli0.top/static/public/postChatUser.min.js";
        } else if (summaryConfig.isEnableSummary()) {
            scriptUrl = "https://ai.tianli0.top/static/public/tianli_gpt.min.js";
            cssLink = "<link rel=\"stylesheet\" href=\"${summary_style}\">";
        }

        if (scriptUrl.isEmpty()) {
            return "";
        }

        String scriptTemplate = """
        <!-- PostChat Plugin start -->
        %s
        <script>
        let tianliGPT_key = '${account_key}';
        let tianliGPT_postSelector = '${summary_postSelector}';
        let tianliGPT_Title = '${summary_title}';
        let tianliGPT_postURL = '${summary_postURL}';
        let tianliGPT_blacklist = '${summary_blacklist}';
        let tianliGPT_wordLimit = '${summary_wordLimit}';
        let tianliGPT_typingAnimate = ${summary_typingAnimate};
        let tianliGPT_theme = '${summary_theme}';
        var postChatConfig = {
          backgroundColor: "${backgroundColor}",
          bottom: "${bottom}",
          left: "${left}",
          fill: "${fill}",
          width: "${width}",
          frameWidth: "${frameWidth}",
          frameHeight: "${frameHeight}",
          defaultInput: ${defaultInput},
          upLoadWeb: ${upLoadWeb},
          showInviteLink: ${showInviteLink},
          userTitle: "${userTitle}",
          userDesc: "${userDesc}",
          addButton: ${addButton},
          beginningText: "${summary_beginningText}",
          userMode: "${userMode}",
          userIcon: "${userIcon}",
          defaultChatQuestions: [${defaultChatQuestions}],
          defaultSearchQuestions: [${defaultSearchQuestions}]
        };
        </script>
        <script data-postChat_key="${account_key}" src="%s"></script>
        <!-- PostChat Plugin end -->
        """;

        String script = String.format(scriptTemplate, cssLink, scriptUrl);

        return PROPERTY_PLACEHOLDER_HELPER.replacePlaceholders(script, properties);
    }
}