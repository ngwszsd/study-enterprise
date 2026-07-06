---
title: "Debug Notes"
readMode: optional
priority: medium
category: debug
keywords:
  - debug
  - issue
  - workaround
  - root-cause
  - gotcha
---

# Debug Notes

## Entries



<spec-entry category="debug" keywords="mybatis,blob,bytearray,yjs,notes" date="2026-07-03" title="MyBatis byte[] BLOB mapping pitfall" description="Fix Yjs note snapshot load by wrapping BLOB byte array result" source="main@cbf53b1">

### MyBatis byte[] BLOB mapping pitfall

When an annotation mapper method returns byte[] directly from a single BLOB column, MyBatis may resolve ByteTypeHandler and try to parse the whole blob as one Byte. Wrap the BLOB column in a small result object property, for example SELECT ydoc_state AS ydocState mapped to NoteDocumentState.ydocState, then Base64 encode that property in the service.

</spec-entry>

<spec-entry category="debug" keywords="tiptap,focus,toolbar,rich-text" date="2026-07-03" title="TipTap toolbar focus handling" description="Fix rich text editor focus being stolen by toolbar buttons" source="main@cbf53b1">

### TipTap toolbar focus handling

TipTap/HeroUI toolbar buttons can steal focus from the ProseMirror contenteditable area on mousedown. Keep editor autofocus disabled, call editor.commands.focus() from the editor surface, and preventDefault on toolbar button mousedown so clicking the editor does not focus the Bold button.

</spec-entry>

<spec-entry category="debug" keywords="tiptap,collaboration-caret,hocuspocus,awareness,updateuser" date="2026-07-03" title="TipTap CollaborationCaret updateUser timing" description="Avoid updateUser timing crash in collaborative editor" source="main@cbf53b1">

### TipTap CollaborationCaret updateUser timing

When using @tiptap/extension-collaboration-caret with HocuspocusProvider in React, do not call editor.commands.updateUser from a separate effect before the caret extension is mounted. CollaborationCaret sets provider.awareness user during plugin initialization; updating provider.awareness directly is enough and avoids runtime 'Cannot read properties of null (reading commands)' on collaborative editor pages.

</spec-entry>