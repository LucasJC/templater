package lucasjc.templater;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootApplication
public class Application implements ApplicationRunner {

	public static final String HELP = """
		Templater supports the following options:
		
		 --help: print this help.
		 --config: path for json configuration file (Defaults to ./config.json)
		   example: "--config=C:/templates"
		 --verbose: flag to enable verbose logs.

		Example config.json file:
		  {
		    "sourceFolder": "source",  // source folder where .ftl files will be looked for. Absolute path or relative to config file.
		    "targetFolder": "target",  // target folder where processed files will be left. Absolute path or relative to config file.
		    "parameters": {            // Params to interpolate. Only simple parameters supported, no nested objects
		      "param1": "value1",
		      "param2": "value2"
		    }
		  }
		""";
	private final ObjectMapper om = new ObjectMapper();
	private boolean verbose = false;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (args.containsOption("help")) {
			printHelp();
			return;
		}
		this.verbose = args.containsOption("verbose");
		String configFile = option(args, "config", "config.json");
		TemplaterConfiguration configuration = loadConfiguration(configFile);
		if (null != configuration) {
			Path basePath = new File(configFile).getAbsoluteFile().toPath().getParent();
			process(basePath, configuration);
		}
	}

	private void printHelp() {
		System.out.println(HELP);
	}

	private void process(Path base, TemplaterConfiguration configuration) throws IOException {
		try {
			TemplateProcessor processor = new TemplateProcessor(configuration, verbose);
			processor.process(base);
		} catch (Exception e) {
			System.err.println("Could not process templates: " + configuration.toString());
			if (verbose) {
				e.printStackTrace();
			} else {
				System.err.println("Error: " + e.getMessage());
			}
		}
	}

	private String option(ApplicationArguments args, String key, String defaultValue) {
		List<String> values = args.getOptionValues(key);
		if (null == values || values.isEmpty()) {
			System.out.println("Using default value for [" + key + "]: " + defaultValue);
			return defaultValue;
		}
		return values.get(0);
	}

	private TemplaterConfiguration loadConfiguration(String configFile) {
		try {
			File sourceFile = new File(configFile);
			String configContent = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
			TemplaterConfiguration conf = om.readValue(configContent, TemplaterConfiguration.class);
			if (verbose) {
				System.out.println("Loaded configuration: " + conf.toString());
			}
			conf.validate();
			return conf;
		} catch (Exception e) {
			System.err.println("Could not load configuration file: " + configFile);
			if (verbose) {
				e.printStackTrace();
			} else {
				System.err.println("Error: " + e.getMessage());
			}
			return null;
		}
	}
}
