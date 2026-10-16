package si.terona.nullbrand.command;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class PermissionsRegressionTest {
    @Test void reloadPermissionDoesNotGrantAdministratorPermission() {
        Map<?,?> root=new Yaml().load(getClass().getResourceAsStream("/plugin.yml"));
        Map<?,?> permissions=(Map<?,?>) root.get("permissions");
        Map<?,?> reload=(Map<?,?>) permissions.get("nullbrand.reload");
        assertFalse(reload.containsKey("children"));
        Map<?,?> admin=(Map<?,?>) permissions.get("nullbrand.admin");
        assertEquals(true,((Map<?,?>)admin.get("children")).get("nullbrand.reload"));
    }
}
