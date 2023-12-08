import org.citrusframework.functions.DefaultFunctionLibrary;
import org.citrusframework.functions.FunctionLibrary;
import org.citrusframework.yaks.report.SystemOutTestReporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YaksAutoConfiguration {

    @Bean
    public SystemOutTestReporter systemOutReporter() {
        return new SystemOutTestReporter();
    }

    @Bean
    public FunctionLibrary yaksFunctionLibrary() {
        FunctionLibrary lib = new FunctionLibrary();
        lib.setPrefix("yaks:");
        lib.getMembers().putAll(new DefaultFunctionLibrary().getMembers());
        return lib;
    }
}
