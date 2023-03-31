package lucasjc.templater;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
public class TemplaterConfiguration {
	private String sourceFolder;
	private String targetFolder;
	private Map<String, String> parameters;

	public void validate() {
		if (null == sourceFolder) {
			throw new IllegalArgumentException("configuration.sourceFolder needed");
		}
		if (null == targetFolder) {
			throw new IllegalArgumentException("configuration.targetFolder needed");
		}
		if (null == parameters || parameters.isEmpty()) {
			throw new IllegalArgumentException("configuration.parameters needed");
		}
		if (parameters.keySet().stream().anyMatch(k -> k.contains("-"))) {
			throw new IllegalArgumentException("configuration.parameters keys cant contain '-'");
		}
	}
}
