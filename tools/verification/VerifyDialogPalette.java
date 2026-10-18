import dev.caveslite.util.DialogText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;

public class VerifyDialogPalette {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void colors(Component component){
        TextColor color=component.color();
        check(color==null||color.equals(NamedTextColor.WHITE)||color.equals(NamedTextColor.GRAY)||color.equals(NamedTextColor.DARK_GRAY),"Non-palette color: "+color);
        for(Component child:component.children())colors(child);
    }
    public static void main(String[] args)throws Exception {
        for(String color:new String[]{"&1","&2","&3","&4","&5","&6","&a","&b","&c","&d","&e","&0","&7","&8","&f","§c","&x&f&f&0&0&0&0","&#12ab34"})colors(DialogText.component(color+"Example"));
        String inactive=LegacyComponentSerializer.legacyAmpersand().serialize(DialogText.status("&cMob&r Name",false));
        check(inactive.contains("&m"),"Inactive name must be struck through");
        check(!inactive.contains("&r"),"Name reset must not clear inactive state");
        String active=LegacyComponentSerializer.legacyAmpersand().serialize(DialogText.status("&cMob&m Name",true));
        check(!active.contains("&m"),"Active name must not be struck through");
        check(active.contains("&f"),"Active name must be white");
        for(String code:new String[]{"es","en"}) {
            YamlConfiguration language=new YamlConfiguration();language.load(Path.of(args[0],"src/main/resources/lang/"+code+".yml").toFile());
            for(String key:language.getKeys(true))if((key.startsWith("admindialogs.")||key.startsWith("dialogs.")||key.startsWith("nether.")||key.startsWith("giant-ui."))&&language.get(key) instanceof String text)colors(DialogText.component(text));
        }
        System.out.println("PASS: "+checks+" dialog palette checks, including both languages, legacy/RGB colors and active/inactive formatting");
    }
}
