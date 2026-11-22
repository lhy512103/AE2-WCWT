import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;
import org.spongepowered.asm.service.*;

public class OfflineMixinCheck {
    public static class Service extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
        public String getName() { return "WCWT offline BC verification"; }
        public boolean isValid() { return true; }
        public IClassProvider getClassProvider() { return this; }
        public IClassBytecodeProvider getBytecodeProvider() { return this; }
        public ITransformerProvider getTransformerProvider() { return null; }
        public IClassTracker getClassTracker() { return null; }
        public IMixinAuditTrail getAuditTrail() { return null; }
        public Collection<String> getPlatformAgents() { return List.of(); }
        public IContainerHandle getPrimaryContainer() { return new ContainerHandleVirtual("wcwt-offline"); }
        public InputStream getResourceAsStream(String name) { return OfflineMixinCheck.class.getClassLoader().getResourceAsStream(name); }
        public URL[] getClassPath() { return new URL[0]; }
        public Class<?> findClass(String name) throws ClassNotFoundException { return findClass(name, false); }
        public Class<?> findClass(String name, boolean init) throws ClassNotFoundException { return Class.forName(name, init, OfflineMixinCheck.class.getClassLoader()); }
        public Class<?> findAgentClass(String name, boolean init) throws ClassNotFoundException { return findClass(name, init); }
        public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException { return getClassNode(name, false); }
        public ClassNode getClassNode(String name, boolean ignored) throws ClassNotFoundException, IOException { return getClassNode(name, ignored, 0); }
        public ClassNode getClassNode(String name, boolean ignored, int flags) throws ClassNotFoundException, IOException {
            try (InputStream stream = getResourceAsStream(name.replace('.', '/') + ".class")) {
                if (stream == null) throw new ClassNotFoundException(name);
                ClassNode node = new ClassNode(); new ClassReader(stream).accept(node, flags); return node;
            }
        }
        public IMixinTransformer transformer() { return getInternal(IMixinTransformerFactory.class).createTransformer(); }
    }
    public static class Properties implements IGlobalPropertyService {
        private record Key(String value) implements IPropertyKey {}
        private final Map<IPropertyKey,Object> values = new HashMap<>();
        public IPropertyKey resolveKey(String key) { return new Key(key); }
        @SuppressWarnings("unchecked") public <T> T getProperty(IPropertyKey key) { return (T) values.get(key); }
        public void setProperty(IPropertyKey key, Object value) { values.put(key,value); }
        @SuppressWarnings("unchecked") public <T> T getProperty(IPropertyKey key, T fallback) { return (T) values.getOrDefault(key,fallback); }
        public String getPropertyString(IPropertyKey key, String fallback) { return String.valueOf(values.getOrDefault(key,fallback)); }
    }
    private static void require(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        MixinBootstrap.init();
        MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
        env.setSide(MixinEnvironment.Side.SERVER);
        MixinEnvironment.setCompatibilityLevel(MixinEnvironment.CompatibilityLevel.JAVA_21);
        Mixins.addConfiguration("bc-offline.mixins.json");
        IMixinTransformer transformer = ((Service) MixinService.getService()).transformer();
        String target = "net.bettercombat.logic.PlayerAttackHelper";
        if (args.length > 0 && args[0].equals("absent")) {
            ClassNode dummy = new ClassNode(); dummy.version = 65; dummy.access = Opcodes.ACC_PUBLIC;
            dummy.name = "test/NoBetterCombat"; dummy.superName = "java/lang/Object";
            ClassWriter writer = new ClassWriter(0); dummy.accept(writer);
            transformer.transformClass(env,"test.NoBetterCombat",writer.toByteArray());
            require(Mixins.getConfigs().stream().allMatch(config -> config.isVisited()), "optional config not visited");
            System.out.println("PASS: required config with @Pseudo target absent prepared without BC linkage");
            return;
        }
        byte[] original;
        try(InputStream in = OfflineMixinCheck.class.getClassLoader().getResourceAsStream(target.replace('.','/')+".class")) { original = Objects.requireNonNull(in).readAllBytes(); }
        byte[] transformed = transformer.transformClass(env,target,original);
        ClassNode node = new ClassNode(); new ClassReader(transformed).accept(node,0);
        MethodNode method = node.methods.stream().filter(m -> m.name.equals("swapHandAttributes") && m.desc.equals("(Lnet/minecraft/world/entity/player/Player;ZLjava/lang/Runnable;)V")).findFirst().orElseThrow();
        int calls=0, remaining=0;
        for(AbstractInsnNode insn : method.instructions) if(insn instanceof MethodInsnNode call) {
            if(call.name.contains("wcwt$setSwappedHand")) calls++;
            if(call.owner.equals("net/minecraft/core/NonNullList") && call.name.equals("set")) remaining++;
        }
        require(calls==4,"expected four real Mixin redirects, got "+calls);
        require(remaining==0,"unredirected list writes remain: "+remaining);
        new Analyzer<>(new BasicVerifier()).analyze(node.name,method);
        Files.write(Path.of(args[0]),transformed);
        System.out.println("PASS: actual BC 2.4.0 class transformed by Mixin 0.8.7; four redirects, zero original set calls; ASM BasicVerifier passed");
    }
}
