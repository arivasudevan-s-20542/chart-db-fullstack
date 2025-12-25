# 📋 Custom Export & DBML Viewer - Implementation Guide

## ✅ What Has Been Implemented

### 🎯 **Feature 1: Data Dictionary XML Export**

A complete custom export format that transforms your database schema into a detailed XML data dictionary matching your template structure.

#### **What It Does**
- Exports database diagrams as XML with full schema details
- Matches your achievement system template structure exactly
- Includes tables, columns, primary keys, foreign keys, and indexes
- Supports all data types, nullable fields, defaults, and constraints
- Automatically generates proper XML formatting with escaping

#### **How to Use It**

1. **Via Menu Bar**:
   ```
   Actions → Export as → Data Dictionary (XML)
   ```

2. **What You Get**:
   - XML file named `{diagram-name}.xml`
   - Complete schema documentation
   - All relationships and constraints
   - Properly formatted for your data dictionary standard

#### **Files Created**:
- `/src/lib/data/xml-export/export-data-dictionary.ts` - Core export logic
- `/src/dialogs/export-data-dictionary-dialog/export-data-dictionary-dialog.tsx` - UI dialog

#### **Features**:
- ✅ Full table schema export
- ✅ Column definitions with data types
- ✅ Primary key constraints
- ✅ Foreign key relationships with CASCADE rules
- ✅ Index definitions
- ✅ Auto-increment/unique value generation
- ✅ Comments and descriptions
- ✅ XML escaping for special characters
- ✅ Live preview before download
- ✅ Respects diagram filters

---

### 🎯 **Feature 2: DBML Viewer Tool**

A standalone DBML viewer and editor accessible from the main menu.

#### **What It Does**
- View and validate DBML (Database Markup Language) files
- Paste DBML content directly or upload `.dbml` files
- Syntax-highlighted preview
- Side-by-side input/output view
- Integrated into the IDE navigation

#### **How to Use It**

1. **Access via Menu**:
   ```
   Tools → DBML Viewer
   ```

2. **Two Input Methods**:
   - **Upload**: Click "Upload DBML File" button
   - **Paste**: Type or paste directly into the left panel

3. **Features**:
   - Real-time syntax highlighting
   - File upload support (.dbml, .txt)
   - Split-view interface
   - Return to editor with back button

#### **Files Created**:
- `/src/pages/dbml-viewer-page/dbml-viewer-page.tsx` - Full viewer implementation
- Route added to `/src/router.tsx`

#### **Example DBML**:
```dbml
Table users {
  id integer [primary key]
  username varchar
  email varchar [unique]
  created_at timestamp
}

Table posts {
  id integer [primary key]
  user_id integer [ref: > users.id]
  title varchar
  content text
}
```

---

## 🔒 **Data Safety Guarantee**

### **Will Editing Code Clear IndexedDB?**

| Scenario | IndexedDB Safe? | Explanation |
|----------|-----------------|-------------|
| ✅ Hot Module Reload (HMR) | **YES** | Vite only swaps changed JS modules; storage untouched |
| ✅ Full page refresh | **YES** | IndexedDB persists in browser profile |
| ✅ Code changes (components, logic) | **YES** | No schema changes = no data impact |
| ✅ `npm run build` | **YES** | Build process never touches browser storage |
| ⚠️ Dexie version bump | **DEPENDS** | Only if migration logic transforms data |
| ❌ Clear browser data | **NO** | Manual action wipes all site data |
| ❌ Incognito mode | **NO** | Data discarded on window close |

### **Storage Location**

Your diagram data lives in:
```
~/.config/Chromium/Default/IndexedDB/
```

This is **persistent browser storage**, not affected by:
- Code edits
- Hot reloads
- Server restarts
- Git operations
- npm installs

---

## 🚀 **How It All Works Together**

### **1. Data Dictionary Export Flow**

```
User clicks "Export" 
  → Dialog opens with loading state
  → exportDataDictionary() reads IndexedDB
  → Transforms tables/fields/relationships to XML
  → Displays preview in CodeSnippet
  → User clicks "Download XML"
  → Browser downloads {diagram-name}.xml
```

### **2. DBML Viewer Flow**

```
User clicks "Tools → DBML Viewer"
  → Navigate to /dbml-viewer route
  → Split-view page loads
  → User uploads file OR pastes content
  → Left panel shows editable input
  → Right panel shows syntax-highlighted preview
```

### **3. Integration Points**

All features are wired through the existing dialog system:

```typescript
// Dialog context automatically provides:
useDialog() → {
  openExportDataDictionaryDialog() // New!
  closeExportDataDictionaryDialog() // New!
  openExportSQLDialog() // Existing
  ...
}
```

Menu integration follows the existing pattern:
```tsx
<MenubarItem onClick={openExportDataDictionaryDialog}>
  Data Dictionary (XML)
</MenubarItem>
```

---

## 🧪 **Testing the Features**

### **Test Data Dictionary Export**

1. Create a test diagram with 2-3 tables
2. Add relationships between them
3. Navigate to: **Actions → Export as → Data Dictionary (XML)**
4. Verify XML preview appears
5. Click "Download XML"
6. Open downloaded file - should match your template structure

### **Test DBML Viewer**

1. Click **Tools → DBML Viewer**
2. Paste this sample DBML:
   ```dbml
   Table achievement_categories {
     category_id bigint [pk, increment]
     category_code varchar(50) [not null]
     category_name varchar(100) [not null]
     is_active boolean [default: true]
   }
   ```
3. Verify syntax highlighting in right panel
4. Click "Upload DBML File" and test file upload
5. Click back arrow to return to editor

---

## 📂 **File Structure**

```
src/
├── lib/
│   └── data/
│       └── xml-export/
│           └── export-data-dictionary.ts    # XML export logic
├── dialogs/
│   └── export-data-dictionary-dialog/
│       └── export-data-dictionary-dialog.tsx # Export dialog UI
├── pages/
│   └── dbml-viewer-page/
│       └── dbml-viewer-page.tsx             # DBML viewer page
├── context/
│   └── dialog-context/
│       ├── dialog-context.tsx               # Updated with new dialog
│       └── dialog-provider.tsx              # Updated with new dialog
└── router.tsx                               # Added /dbml-viewer route
```

---

## 🎨 **Customization Options**

### **Modify XML Template**

Edit `/src/lib/data/xml-export/export-data-dictionary.ts`:

```typescript
// Change XML structure
function generateTableXML(table: DBTable, diagram: Diagram): string {
    return `<your-custom-format>
        ${/* your template here */}
    </your-custom-format>`;
}
```

### **Add More Export Formats**

Follow the same pattern:
1. Create `/src/lib/data/{format}-export/export-{format}.ts`
2. Create dialog in `/src/dialogs/export-{format}-dialog/`
3. Add to dialog context
4. Wire into menu

---

## 🐛 **Troubleshooting**

### **Issue: Dialog doesn't open**

**Check**:
```typescript
// In browser console:
localStorage.getItem('ChartDB')
// Should show IndexedDB is accessible
```

### **Issue: XML export is empty**

**Verify**:
- Diagram has tables
- Tables have fields
- No diagram filter is hiding all tables

### **Issue: DBML viewer route 404**

**Confirm**:
```bash
npm run dev
# Check console for route registration
```

---

## 🎯 **Next Steps / Future Enhancements**

### **Potential Additions**:

1. **Import from XML**
   - Reverse parser: XML → Diagram
   - Validation and schema mapping
   
2. **DBML → Diagram Conversion**
   - Parse DBML in viewer
   - Generate ChartDB diagram
   - Import directly to editor

3. **Export Templates Library**
   - Multiple XML format presets
   - User-defined templates
   - Template marketplace

4. **Real-time Collaboration**
   - Replace IndexedDB with Supabase
   - Multi-user editing
   - Cloud sync

---

## 📞 **Support**

If you encounter issues:

1. Check browser console for errors
2. Verify IndexedDB is enabled in browser
3. Test with a simple 1-table diagram first
4. Clear browser cache and retry

---

## 🎉 **Summary**

✅ **Data Dictionary XML Export** - Fully functional, matches your template  
✅ **DBML Viewer** - Standalone tool accessible from menu  
✅ **IndexedDB Safety** - Data persists through code changes  
✅ **Complete Integration** - Wired into existing UI patterns  
✅ **Production Ready** - Error handling, loading states, validation  

**All features are ready to use!** 🚀
