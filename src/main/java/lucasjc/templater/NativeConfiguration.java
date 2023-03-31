package lucasjc.templater;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@RegisterReflectionForBinding({ TemplaterConfiguration.class })
public class NativeConfiguration {
	// empty
}

