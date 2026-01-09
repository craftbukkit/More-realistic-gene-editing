# More Realistic Gene Editing - 安全性、性能与漏洞审查报告

## 📋 审查概述

**审查日期**: 2026-01-08  
**审查版本**: 0.2.0-alpha  
**审查范围**: 全部Java源代码、网络通信、数据存储

---

## 🔒 安全性审查

### 1. 网络安全

#### 1.1 C2S数据包验证 ⚠️ 需改进

**问题**: 部分C2S数据包缺乏充分的服务器端验证

**文件**: `network/c2s/*.java`

**建议修复**:
```java
// 在 C2SRequestGenomeSlicePacket.java 中添加验证
public void handle(ServerPlayNetworkHandler handler) {
    ServerPlayerEntity player = handler.player;
    
    // 1. 验证请求长度限制
    if (length > MAX_SLICE_LENGTH) {
        LOGGER.warn("Player {} requested slice too large: {}", player.getName(), length);
        return;
    }
    
    // 2. 验证玩家权限
    if (!canPlayerAccessGenome(player, genomeId)) {
        LOGGER.warn("Player {} unauthorized genome access: {}", player.getName(), genomeId);
        return;
    }
    
    // 3. 速率限制检查
    if (RateLimiters.isRateLimited(player.getUuid(), "genome_slice")) {
        return;
    }
}
```

#### 1.2 速率限制 ✅ 已实现

**文件**: `security/RateLimiters.java`

当前实现的速率限制器可以有效防止请求泛滥攻击。

#### 1.3 输入验证 ⚠️ 需加强

**问题**: 部分用户输入未经充分过滤

**建议**: 在以下位置添加输入验证:
- GUI文本输入框 (搜索框、坐标输入)
- NBT数据反序列化
- 配置文件解析

### 2. 数据安全

#### 2.1 NBT数据处理 ⚠️ 需改进

**文件**: `util/SafeNBT.java`

**当前问题**: 
- 缺乏NBT大小限制检查
- 可能导致内存耗尽攻击

**建议修复**:
```java
public class SafeNBT {
    private static final int MAX_NBT_SIZE = 2 * 1024 * 1024; // 2MB
    private static final int MAX_STRING_LENGTH = 32767;
    private static final int MAX_ARRAY_LENGTH = 65535;
    
    public static NbtCompound safeRead(NbtCompound nbt) {
        // 验证NBT大小
        if (estimateSize(nbt) > MAX_NBT_SIZE) {
            throw new IllegalArgumentException("NBT data too large");
        }
        return nbt;
    }
    
    public static String safeGetString(NbtCompound nbt, String key) {
        String value = nbt.getString(key);
        if (value.length() > MAX_STRING_LENGTH) {
            return value.substring(0, MAX_STRING_LENGTH);
        }
        return value;
    }
}
```

#### 2.2 基因组数据存储 ✅ 安全

当前设计使用确定性生成 + Patch存储，不会产生大文件安全问题。

### 3. 权限控制

#### 3.1 方块交互权限 ⚠️ 需添加

**建议**: 添加方块保护检查
```java
// 在 LabEquipmentBlock.onUse() 中
@Override
public ActionResult onUse(...) {
    // 检查玩家是否有权限操作此方块
    if (!world.canPlayerModifyAt(player, pos)) {
        return ActionResult.FAIL;
    }
    // ... 原有逻辑
}
```

---

## ⚡ 性能优化

### 1. 内存管理

#### 1.1 基因组缓存 ⚠️ 需优化

**问题**: `MoreRealisticGeneEditing.genomeCache` 使用简单HashMap，可能导致内存泄漏

**建议修复**:
```java
// 使用带有大小限制和过期机制的缓存
public class GenomeCache {
    private static final int MAX_CACHE_SIZE = 100;
    private static final long CACHE_EXPIRE_MS = 5 * 60 * 1000; // 5分钟
    
    private final LinkedHashMap<UUID, CacheEntry> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, CacheEntry> eldest) {
            return size() > MAX_CACHE_SIZE || 
                   eldest.getValue().isExpired();
        }
    };
    
    private record CacheEntry(Genome genome, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_MS;
        }
    }
}
```

#### 1.2 字符串构建 ⚠️ 需优化

**位置**: `genome/Genome.java`, `genome/pcr/PcrSimulator.java`

**问题**: 在循环中使用字符串拼接

**建议**: 使用StringBuilder
```java
// 优化前
String result = "";
for (int i = 0; i < length; i++) {
    result += bases[i];
}

// 优化后
StringBuilder sb = new StringBuilder(length);
for (int i = 0; i < length; i++) {
    sb.append(bases[i]);
}
String result = sb.toString();
```

### 2. 计算优化

#### 2.1 序列迭代器 ✅ 已优化

`Genome.SequenceIterator` 使用二分查找进行seek操作，性能良好。

#### 2.2 PCR模拟 ⚠️ 需优化

**问题**: `PcrSimulator.runPcr()` 在大序列上可能较慢

**建议**:
```java
// 添加序列搜索的KMP或Boyer-Moore算法
private int findPrimerBindingSiteOptimized(String sequence, String pattern) {
    // 使用KMP算法替代简单的indexOf
    int[] lps = computeLPSArray(pattern);
    // ... KMP实现
}
```

### 3. 网络优化

#### 3.1 数据包压缩 ⚠️ 建议添加

**建议**: 对大型基因组切片数据进行压缩
```java
public class GenomeSlicePacket {
    public void write(PacketByteBuf buf) {
        byte[] compressed = compress(packedBases);
        buf.writeVarInt(compressed.length);
        buf.writeBytes(compressed);
    }
    
    private byte[] compress(byte[] data) {
        if (data.length < 100) return data; // 小数据不压缩
        
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(data);
        deflater.finish();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            baos.write(buffer, 0, count);
        }
        return baos.toByteArray();
    }
}
```

#### 3.2 批量请求 ⚠️ 建议添加

**建议**: 允许客户端批量请求多个基因组切片
```java
public record C2SBatchGenomeSliceRequest(
    UUID genomeId,
    List<SliceRange> ranges
) {
    public record SliceRange(long start, int length) {}
}
```

### 4. 渲染优化

#### 4.1 GUI渲染 ⚠️ 需优化

**位置**: `screens/GenomeTerminalScreen.java`

**问题**: 每帧重新计算序列渲染

**建议**: 添加渲染缓存
```java
private String cachedSequence;
private long cachedStart;
private int cachedLength;

private void renderSequence(DrawContext context) {
    if (needsRefresh()) {
        cachedSequence = genome.getSequence(viewStart, viewLength);
        cachedStart = viewStart;
        cachedLength = viewLength;
    }
    // 使用缓存的序列渲染
}
```

---

## 🐛 潜在漏洞

### 1. 整数溢出 ⚠️ 风险

**位置**: `genome/Genome.java`

**问题**: 长序列位置计算可能溢出

**建议修复**:
```java
public String getSequence(long start, int length) {
    // 添加边界检查
    if (start < 0 || length < 0) {
        throw new IllegalArgumentException("Invalid range");
    }
    if (start > Long.MAX_VALUE - length) {
        throw new IllegalArgumentException("Position overflow");
    }
    // ...
}
```

### 2. 空指针异常 ⚠️ 风险

**位置**: 多处

**建议**: 添加null检查和Optional使用
```java
// 使用Optional避免NPE
public Optional<Genome> getGenome(UUID id) {
    return Optional.ofNullable(genomeCache.get(id));
}
```

### 3. 资源泄漏 ⚠️ 风险

**位置**: `genome/provider/EnsemblRestProvider.java`

**问题**: HTTP连接可能未正确关闭

**建议**: 使用try-with-resources

### 4. 线程安全 ⚠️ 风险

**位置**: `project/ServerProjectManager.java`

**问题**: 多线程访问玩家数据可能导致竞态条件

**建议**:
```java
private static final ConcurrentHashMap<UUID, PlayerProjectState> playerStates = 
    new ConcurrentHashMap<>();
```

---

## 📊 优化优先级

| 问题 | 严重性 | 优先级 | 预计工时 |
|------|--------|--------|----------|
| C2S数据包验证 | 高 | P0 | 4h |
| NBT大小限制 | 高 | P0 | 2h |
| 基因组缓存优化 | 中 | P1 | 3h |
| 方块权限检查 | 中 | P1 | 2h |
| 字符串构建优化 | 低 | P2 | 1h |
| 数据包压缩 | 低 | P2 | 4h |
| GUI渲染缓存 | 低 | P2 | 2h |

---

## ✅ 已修复的问题

### 在本次更新中修复:

1. **Minecraft 1.21 API兼容性** - 更新所有Registry引用
2. **物品组注册** - 使用新的ItemGroupEvents API
3. **命令注册** - 使用CommandRegistrationCallback v2 API
4. **方块实体基类** - 添加了完整的侧向物品栏支持

---

## 📝 建议的代码改进

### 添加以下工具类:

```java
// 1. 安全的NBT工具
public final class SafeNbtHelper {
    public static int getIntClamped(NbtCompound nbt, String key, int min, int max) {
        return Math.max(min, Math.min(max, nbt.getInt(key)));
    }
}

// 2. 性能监控工具
public final class PerformanceMonitor {
    private static final Map<String, Long> timings = new ConcurrentHashMap<>();
    
    public static void startTiming(String key) {
        timings.put(key, System.nanoTime());
    }
    
    public static long endTiming(String key) {
        Long start = timings.remove(key);
        return start != null ? System.nanoTime() - start : 0;
    }
}

// 3. 输入验证工具
public final class InputValidator {
    public static boolean isValidDnaSequence(String seq) {
        return seq.matches("^[ACGTacgt]+$");
    }
    
    public static boolean isValidCoordinate(long pos, long max) {
        return pos >= 0 && pos < max;
    }
}
```

---

## 🔧 推荐配置

### 服务器配置建议 (config/mrge-server.toml)

```toml
[security]
# 每个玩家的最大活跃基因组数
max_genomes_per_player = 10

# 基因组切片请求的速率限制 (每秒)
slice_request_rate_limit = 10

# 最大切片长度 (碱基对)
max_slice_length = 10000

[performance]
# 基因组缓存大小
genome_cache_size = 100

# 缓存过期时间 (秒)
cache_expire_seconds = 300

# 启用数据包压缩
enable_packet_compression = true
compression_threshold = 256
```

---

*审查完成日期: 2026-01-08*
*审查员: Claude AI*
