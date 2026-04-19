# PacketEvents Adapter Implementation - Decisions

## Date: 2026-04-19

### Architecture Decisions

#### 1. Package Structure
**Decision:** Place adapter in `optional.packetevents` subpackage
**Rationale:** 
- Clearly indicates optional nature
- Isolates PacketEvents-dependent code
- Easy to exclude if needed

#### 2. Class Loading Safety
**Decision:** Use lazy initialization pattern
**Rationale:**
- Prevents NoClassDefFoundError when PacketEvents absent
- Allows safe instantiation of adapter regardless of PacketEvents presence
- Core plugin class can reference adapter without risk

#### 3. API Design for Preferences
**Decision:** Builder pattern + direct property setters
**Rationale:**
- Builders provide fluent API for complex configurations
- Direct setters allow simple programmatic changes
- Both patterns support method chaining

#### 4. Item Processing Strategy
**Decision:** Clone-and-modify pattern
**Rationale:**
- Never modifies original items (server state preserved)
- Safe to apply player-specific changes
- Easy to rollback if needed

#### 5. Lore Formatting Approach
**Decision:** Consumer-based custom formatters
**Rationale:**
- Allows complete customization by plugins
- Type-safe with VisualContext parameter
- Can be registered per-enchantment

### Dependency Management

#### Build Configuration
- PacketEvents: `compileOnly` (not included in output)
- This ensures zero runtime dependency on PacketEvents
- Core library works completely standalone

#### Plugin Configuration
- `softdepend: [packetevents]` in plugin.yml
- Plugin loads normally with or without PacketEvents
- Adapter initializes only if PacketEvents detected

### Error Handling Strategy

#### Graceful Degradation
- All PacketEvents interactions wrapped in try-catch
- Failures logged at FINE/WARNING level, not ERROR
- Adapter state reflects availability accurately

#### Null Safety
- All public methods use `@NotNull`/`@Nullable` annotations
- `Objects.requireNonNull()` for parameter validation
- Consistent null handling throughout

### Thread Safety

#### Concurrent Collections
- `ConcurrentHashMap` for player preferences
- Thread-safe for read-heavy operations
- Write operations synchronized at higher level

#### Packet Processing
- PacketEvents handles synchronization
- Item processing is stateless
- Safe for concurrent packet handling
