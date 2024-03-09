package io.izzel.arclight.boot.mod;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileParser;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class ArclightLocator_Forge implements IModLocator {

    private final IModFile arclight;

    public ArclightLocator_Forge() {
        ModBootstrap.run();
        this.arclight = loadJar();
    }

    @Override
    public List<ModFileOrException> scanMods() {
        ArclightJarInJarAdaptor.inject();
        return List.of(new ModFileOrException(arclight, null));
    }

    @Override
    public String name() {
        return "arclight";
    }

    @Override
    public void scanFile(IModFile file, Consumer<Path> pathConsumer) {
        final Function<Path, SecureJar.Status> status = p -> file.getSecureJar().verifyPath(p);
        try (Stream<Path> files = Files.find(file.getSecureJar().getRootPath(), Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0 && p.getFileName().toString().endsWith(".class"))) {
            file.setSecurityStatus(files.peek(pathConsumer).map(status).reduce((s1, s2) -> SecureJar.Status.values()[Math.min(s1.ordinal(), s2.ordinal())]).orElse(SecureJar.Status.INVALID));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }

    protected IModFile loadJar() {
        try {
            var version = System.getProperty("arclight.version");
            var path = Paths.get(".arclight", "mod_file", version + ".jar");

            return new ModFile(SecureJar.from(it -> excludePackages(it, version), path), this, ModFileParser::modsTomlParser);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final Set<String> EXCLUDES = Set.of(
        "net.minecraft.world.level.block"
    );

    private JarMetadata excludePackages(SecureJar secureJar, String version) {
        secureJar.getPackages().removeIf(it -> EXCLUDES.stream().anyMatch(it::startsWith));
        return new SimpleJarMetadata("arclight", version.substring(version.indexOf('-') + 1), secureJar.getPackages(), List.of());
    }
}
