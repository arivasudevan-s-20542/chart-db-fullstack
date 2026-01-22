# Building a Framework Compiler in Go - Knowledge Roadmap

## 1. Go Language Fundamentals

### Must Know
- [ ] Variables, types, structs, interfaces
- [ ] Slices, maps, strings, runes (Unicode handling)
- [ ] Error handling patterns
- [ ] File I/O (`os`, `io`, `bufio` packages)
- [ ] String manipulation (`strings`, `bytes` packages)
- [ ] Regular expressions (`regexp` package)
- [ ] Testing (`testing` package)
- [ ] Goroutines & channels (parallelism)
- [ ] CLI building (`flag` or `cobra` library)

### Resources
- [Go Tour](https://tour.golang.org)
- [Go by Example](https://gobyexample.com)
- [Effective Go](https://golang.org/doc/effective_go)

---

## 2. Compiler Theory

### Phases of Compilation
```
Source Code → Lexer → Tokens → Parser → AST → Transformer → CodeGen → Output
```

### 2.1 Lexical Analysis (Lexer/Tokenizer)
- [ ] What is a token
- [ ] Token types (identifiers, keywords, operators, literals)
- [ ] Reading characters from source
- [ ] Handling whitespace, newlines
- [ ] String/template literal handling
- [ ] Comment handling
- [ ] Error reporting with line/column numbers

### 2.2 Parsing (Parser)
- [ ] What is an AST (Abstract Syntax Tree)
- [ ] Recursive descent parsing
- [ ] Operator precedence (Pratt parsing)
- [ ] Lookahead techniques
- [ ] Error recovery strategies
- [ ] Grammar rules (BNF/EBNF notation)

### 2.3 Semantic Analysis
- [ ] Scope management (variable scoping)
- [ ] Symbol tables
- [ ] Type checking (if applicable)
- [ ] Name resolution

### 2.4 Code Generation
- [ ] AST traversal patterns (visitor pattern)
- [ ] Generating valid JavaScript
- [ ] Source maps (mapping output to input)
- [ ] Pretty printing vs minification

### Resources
- Book: "Writing An Interpreter In Go" by Thorsten Ball
- Book: "Writing A Compiler In Go" by Thorsten Ball
- Book: "Crafting Interpreters" by Robert Nystrom (free online)

---

## 3. JavaScript/Template Syntax Knowledge

### JavaScript Syntax to Parse
- [ ] Variables (`var`, `let`, `const`)
- [ ] Functions (declaration, expression, arrow)
- [ ] Classes
- [ ] Modules (`import`, `export`)
- [ ] Operators (arithmetic, logical, ternary)
- [ ] Control flow (`if`, `for`, `while`, `switch`)
- [ ] Objects and arrays
- [ ] Template literals
- [ ] Destructuring
- [ ] Spread operator
- [ ] async/await

### Template Syntax to Parse
- [ ] Interpolation (`{{expression}}`)
- [ ] Conditionals (`{{#if}}`, `{{else}}`)
- [ ] Loops (`{{#each}}`, `{{#for}}`)
- [ ] Component invocation
- [ ] Event bindings
- [ ] Attribute bindings
- [ ] Slots/yield
- [ ] Comments

### Resources
- [ECMAScript Specification](https://tc39.es/ecma262/)
- [Handlebars Spec](https://handlebarsjs.com/guide/)
- [ESTree AST Spec](https://github.com/estree/estree)

---

## 4. Bundler Concepts

### Module Resolution
- [ ] Resolving `import` paths
- [ ] Node.js resolution algorithm
- [ ] Handling `node_modules`
- [ ] Alias/path mapping

### Bundling
- [ ] Dependency graph building
- [ ] Topological sorting
- [ ] Circular dependency handling
- [ ] Code splitting
- [ ] Tree shaking (dead code elimination)

### Output Generation
- [ ] IIFE wrapping
- [ ] Module formats (ESM, CJS, UMD)
- [ ] Chunk naming
- [ ] Asset handling (CSS, images)

---

## 5. Framework-Specific Concepts

### Component Model
- [ ] Component lifecycle
- [ ] Props/attributes
- [ ] State management
- [ ] Reactivity system
- [ ] Event handling

### Templating
- [ ] Template compilation to render functions
- [ ] Virtual DOM (if applicable)
- [ ] Diffing algorithms
- [ ] Hydration (SSR)

### Routing
- [ ] Route definitions
- [ ] Route matching
- [ ] History API

---

## 6. Tooling & Infrastructure

### CLI Development
- [ ] Command parsing
- [ ] Flags and arguments
- [ ] Help text generation
- [ ] Configuration file loading (JSON, YAML)
- [ ] Watch mode (file system events)

### Development Server
- [ ] HTTP server in Go
- [ ] Hot Module Replacement (HMR) concepts
- [ ] WebSocket for live reload
- [ ] Serving static files

### Build Optimization
- [ ] Caching strategies
- [ ] Incremental compilation
- [ ] Parallel processing
- [ ] Minification

---

## 7. Project Architecture

### Suggested Structure
```
lyte-go/
├── cmd/lyte/main.go         # CLI entry point
├── pkg/
│   ├── lexer/               # Tokenization
│   ├── parser/              # AST generation
│   ├── ast/                 # AST node definitions
│   ├── analyzer/            # Semantic analysis
│   ├── transformer/         # AST transformations
│   ├── codegen/             # JavaScript output
│   ├── bundler/             # Module bundling
│   ├── resolver/            # Path resolution
│   ├── watcher/             # File watching
│   └── server/              # Dev server
├── internal/                # Private utilities
├── testdata/                # Test fixtures
└── docs/                    # Documentation
```

---

## 8. Learning Path (Recommended Order)

### Phase 1: Foundations (2-4 weeks)
1. Go basics + string manipulation
2. Read "Writing An Interpreter In Go" chapters 1-2
3. Build a calculator lexer/parser

### Phase 2: Simple Lexer (2-3 weeks)
1. Tokenize HTML: `<div class="foo">Hello</div>`
2. Add template syntax: `{{name}}`
3. Handle edge cases, errors

### Phase 3: Parser (3-4 weeks)
1. Parse HTML into AST
2. Parse template expressions
3. Parse JavaScript expressions inside templates

### Phase 4: Code Generator (2-3 weeks)
1. AST → JavaScript render functions
2. Handle all template constructs
3. Source map basics

### Phase 5: Bundler (3-4 weeks)
1. Module resolution
2. Dependency graph
3. Output generation

### Phase 6: Dev Experience (2-3 weeks)
1. CLI tool
2. Watch mode
3. Dev server

---

## 9. Reference Implementations to Study

| Project | Language | What to Learn |
|---------|----------|---------------|
| esbuild | Go | Fast bundler architecture |
| Svelte | JS | Template compilation |
| Vue Compiler | JS | Template → render function |
| Glimmer | JS | Ember's template compiler |
| swc | Rust | Fast JS/TS parser |

---

## 10. Quick Reference: Go Patterns for Compilers

### Lexer Pattern
```go
type Lexer struct {
    input   string
    pos     int
    readPos int
    ch      byte
}

func (l *Lexer) NextToken() Token { ... }
```

### AST Node Pattern
```go
type Node interface {
    TokenLiteral() string
}

type Expression interface {
    Node
    expressionNode()
}

type Statement interface {
    Node
    statementNode()
}
```

### Visitor Pattern (for codegen)
```go
type Visitor interface {
    VisitElement(node *Element) string
    VisitText(node *Text) string
    VisitExpression(node *Expression) string
}
```

---

## Timeline Estimate

| Phase | Duration | Outcome |
|-------|----------|---------|
| Learning | 1-2 months | Understand concepts |
| Basic Compiler | 3-4 months | Parse templates, generate JS |
| Full Featured | 6-12 months | Production-quality tool |
| Optimized | 1-2 years | Performance parity with JS version |

---

## Next Steps

1. [ ] Set up Go development environment
2. [ ] Complete Go Tour
3. [ ] Read "Writing An Interpreter In Go" (first 3 chapters)
4. [ ] Build a simple expression lexer/parser
5. [ ] Study your framework's public syntax
6. [ ] Start building!

---

*Good luck! 🚀*
