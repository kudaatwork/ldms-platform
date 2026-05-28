/** Lazy-loaded CodeMirror + Prettier (avoids Vite prebundle chunk errors on notifications route). */
export interface NotificationEmailEditorDeps {
  html: typeof import('@codemirror/lang-html').html;
  HighlightStyle: typeof import('@codemirror/language').HighlightStyle;
  syntaxHighlighting: typeof import('@codemirror/language').syntaxHighlighting;
  Compartment: typeof import('@codemirror/state').Compartment;
  EditorState: typeof import('@codemirror/state').EditorState;
  EditorView: typeof import('@codemirror/view').EditorView;
  basicSetup: typeof import('codemirror').basicSetup;
  tags: typeof import('@lezer/highlight').tags;
  prettierFormat: typeof import('prettier/standalone').format;
  prettierHtmlPlugin: NonNullable<import('prettier').Plugin[]>[number];
  prettierPostcssPlugin: NonNullable<import('prettier').Plugin[]>[number];
}

let loadPromise: Promise<NotificationEmailEditorDeps> | null = null;

export function loadNotificationEmailEditorDeps(): Promise<NotificationEmailEditorDeps> {
  if (!loadPromise) {
    loadPromise = Promise.all([
      import('@codemirror/lang-html'),
      import('@codemirror/language'),
      import('@codemirror/state'),
      import('@codemirror/view'),
      import('codemirror'),
      import('@lezer/highlight'),
      import('prettier/standalone'),
      import('prettier/plugins/html'),
      import('prettier/plugins/postcss'),
    ]).then(
      ([
        langHtml,
        language,
        state,
        view,
        codemirror,
        lezer,
        prettier,
        prettierHtml,
        prettierPostcss,
      ]) => ({
        html: langHtml.html,
        HighlightStyle: language.HighlightStyle,
        syntaxHighlighting: language.syntaxHighlighting,
        Compartment: state.Compartment,
        EditorState: state.EditorState,
        EditorView: view.EditorView,
        basicSetup: codemirror.basicSetup,
        tags: lezer.tags,
        prettierFormat: prettier.format,
        prettierHtmlPlugin: prettierHtml.default,
        prettierPostcssPlugin: prettierPostcss.default,
      }),
    );
  }
  return loadPromise;
}
