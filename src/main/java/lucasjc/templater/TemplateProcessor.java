package lucasjc.templater;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class TemplateProcessor {
	private final boolean verbose;
	private final TemplaterConfiguration configuration;
	private Configuration freemarker;

	public TemplateProcessor(TemplaterConfiguration configuration, boolean verbose) {
		this.configuration = configuration;
		this.verbose = verbose;
	}

	public void process(Path base) throws IOException {
		File source = buildAbsoluteFile(configuration.getSourceFolder(), base);
		if (!source.exists()) {
			throw new IllegalArgumentException("Source folder does not exist: " + source.getAbsolutePath());
		}
		setupFreemarker(source);
		File target = buildAbsoluteFile(configuration.getTargetFolder(), base);
		Path targetPath = Path.of(target.getAbsolutePath());
		copyFilesToTarget(source, target);
		try (Stream<Path> paths = Files.walk(targetPath)) {
			paths.filter(path ->
					path.toFile().isFile() && path.toFile().getName().endsWith(".ftl")
			).forEach(path -> processFile(Path.of(target.getAbsolutePath()), path.toAbsolutePath(), configuration));
		}
		System.out.println("Done!");
	}

	private void copyFilesToTarget(File source, File target) throws IOException {
		if (!target.exists()) {
			Files.createDirectories(Path.of(target.getAbsolutePath()));
		} else {
			FileUtils.cleanDirectory(target);
		}
		FileUtils.copyDirectory(source, target, null, false, StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.COPY_ATTRIBUTES);
	}

	private File buildAbsoluteFile(String configuration, Path base) {
		File target = new File(configuration);
		if (!target.isAbsolute()) {
			target = base.resolve(configuration).toFile().getAbsoluteFile();
		}
		return target;
	}

	private void setupFreemarker(File source) throws IOException {
		freemarker = new Configuration(Configuration.VERSION_2_3_32);
		freemarker.setDefaultEncoding(StandardCharsets.UTF_8.name());
		freemarker.setTemplateLoader(new FileTemplateLoader(source));
	}

	private void processFile(Path sourcePath, Path filePath, TemplaterConfiguration templaterConfiguration) {
		try (Writer fileWriter = new FileWriter(obtainFileName(filePath, templaterConfiguration))) {
			String relPath = sourcePath.relativize(filePath).toString();
			Template template = freemarker.getTemplate(relPath);
			template.process(templaterConfiguration.getParameters(), fileWriter);
			Files.delete(filePath);
		} catch (Exception e) {
			error("Error processing file: " + filePath, e);
		}
	}

	private String obtainFileName(Path filePath, TemplaterConfiguration templaterConfiguration) throws IOException {
		String fileName = filePath.getFileName().toString();
		fileName = fileName.substring(0, fileName.length() - 4);
		fileName = processFileName(fileName, templaterConfiguration.getParameters());
		return filePath.getParent().resolve(fileName).toString();
	}

	private String processFileName(String sourceString, Map<String, String> params) throws IOException {
		Template template = new Template(sourceString, sourceString, freemarker);
		try(StringWriter writer = new StringWriter()) {
			template.process(params, writer);
			return writer.toString();
		} catch (TemplateException e) {
			error("Error processing file name: " + sourceString, e);
			return sourceString;
		}
	}

	private void error(String message, Throwable t) {
		if (verbose) {
			System.err.println(message);
			t.printStackTrace();
		} else {
			System.err.println(message + " - " + t.getMessage());
		}
	}
}
