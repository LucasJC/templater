package lucasjc.templater;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

class TemplaterTests {

	private static final Logger LOG = LoggerFactory.getLogger(TemplaterTests.class);

	private final PrintStream sout = System.out;
	private final ByteArrayOutputStream soutCapture = new ByteArrayOutputStream();

	@BeforeEach
	void setUp() {
		System.setOut(new PrintStream(soutCapture));
	}

	@Test
	void printsHelp() throws Exception {
		Application app = new Application();
		ApplicationArguments args = new DefaultApplicationArguments("--help");
		app.run(args);
		Assertions.assertEquals(Application.HELP.trim(), soutCapture.toString().trim());
	}

	@Test
	void okExecution() throws Exception {
		File tempFolder = Files.newTemporaryFolder();
		Path sourceFolder = tempFolder.toPath().resolve("sourceFolder");
		Path targetFolder = tempFolder.toPath().resolve("targetFolder");

		String configContent = """
				{
					"sourceFolder": "%s",
					"targetFolder": "%s",
					"parameters": {
						"author": "Lucas",
						"project": "example-project"
					}
				}
				""".formatted(sourceFolder.toString().replace("\\", "/"),
				targetFolder.toString().replace("\\", "/"));
		createFile(tempFolder.toPath(), "config.json", configContent);

		String fileContent = """
				${project} by ${author}
				""";
		createFile(sourceFolder, "test.txt.ftl", fileContent);
		createFile(sourceFolder.resolve("secondary-folder"), "${project}-file.txt.ftl", fileContent);
		//createFile(sourceFolder.resolve("${project}-folder"), "test2.txt.ftl", fileContent);

		String staticContent = """
				static content
				""";
		createFile(sourceFolder, "static.txt", staticContent);

		Application app = new Application();
		String configPath = tempFolder.toPath().resolve("config.json").toString();
		ApplicationArguments args = new DefaultApplicationArguments("--config=" + configPath, "--verbose");
		app.run(args);
		sout.println(soutCapture);

		Assertions.assertEquals(staticContent,
				Files.contentOf(targetFolder.resolve("static.txt").toFile(), StandardCharsets.UTF_8));
		Assertions.assertEquals("example-project by Lucas\n",
				Files.contentOf(targetFolder.resolve("test.txt").toFile(), StandardCharsets.UTF_8));
		Assertions.assertEquals("example-project by Lucas\n",
				Files.contentOf(targetFolder.resolve("secondary-folder/example-project-file.txt").toFile(), StandardCharsets.UTF_8));
	}

	@Test
	void error() throws Exception {
		ByteArrayOutputStream errCapture = new ByteArrayOutputStream();
		System.setErr(new PrintStream(errCapture));

		File tempFolder = Files.newTemporaryFolder();

		createFile(tempFolder.toPath(), "config.json", "invalid json {");

		Application app = new Application();
		String configPath = tempFolder.toPath().resolve("config.json").toString();
		ApplicationArguments args = new DefaultApplicationArguments("--config=" + configPath);
		app.run(args);

		Assertions.assertNotNull(errCapture.toString());
	}

	private void createFile(Path basePath, String fileName, String content) throws IOException {
		File file = basePath.resolve(fileName).toFile();
		FileUtils.write(file, content, StandardCharsets.UTF_8);
		sout.println("Wrote file to " + file);
	}
}
