/**
 * This file was automatically generated by the TRLD transpiler.
 * Source: trld/jsonld/context.py
 */
package trld.jsonld;

//import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.*;

import trld.Builtins;
import trld.KeyValue;

import static trld.Common.loadJson;
import static trld.Common.warning;
import static trld.Common.resolveIri;
import static trld.jsonld.Base.*;

public class Context { // LINE: 68

  public static final String DEFAULT_PROCESSING_MODE = JSONLD11; // LINE: 6
  public static final Integer MAX_REMOTE_CONTEXTS = 512; // LINE: 8

  public Map</*@Nullable*/ String, Term> terms; // LINE: 70
  public String baseIri; // LINE: 71
  public /*@Nullable*/ String originalBaseUrl; // LINE: 72
  public /*@Nullable*/ Map inverseContext; // LINE: 74
  public /*@Nullable*/ String vocabularyMapping; // LINE: 76
  public /*@Nullable*/ String defaultLanguage; // LINE: 77
  public /*@Nullable*/ String defaultBaseDirection; // LINE: 78
  public Boolean propagate; // LINE: 80
  public /*@Nullable*/ Context previousContext; // LINE: 81
  public String processingMode; // LINE: 83
  public /*@Nullable*/ Double version; // LINE: 84

  public Context(/*@Nullable*/ String baseIri) {
    this(baseIri, null);
  }
  public Context(/*@Nullable*/ String baseIri, /*@Nullable*/ String originalBaseUrl) { // LINE: 88
    this.initialize(baseIri, originalBaseUrl); // LINE: 89
  }

  public void initialize(/*@Nullable*/ String baseIri) {
    this.initialize(baseIri, null);
  }
  public void initialize(/*@Nullable*/ String baseIri, /*@Nullable*/ String originalBaseUrl) { // LINE: 91
    this.terms = new HashMap<>(); // LINE: 92
    this.baseIri = (baseIri == null ? "" : baseIri); // LINE: 94
    if (originalBaseUrl != null) { // LINE: 95
      this.originalBaseUrl = originalBaseUrl; // LINE: 96
    } else {
      this.originalBaseUrl = null; // LINE: 98
    }
    this.vocabularyMapping = null; // LINE: 100
    this.defaultLanguage = null; // LINE: 101
    this.defaultBaseDirection = null; // LINE: 102
    this.propagate = true; // LINE: 103
    this.previousContext = null; // LINE: 104
    this.processingMode = DEFAULT_PROCESSING_MODE; // LINE: 105
    this.version = null; // LINE: 106
    this.inverseContext = null; // LINE: 107
  }

  public Context copy() { // LINE: 110
    Context cloned = new Context(this.baseIri, this.originalBaseUrl); // LINE: 111
    cloned.terms = new HashMap(this.terms); // LINE: 112
    cloned.vocabularyMapping = (String) this.vocabularyMapping; // LINE: 113
    cloned.defaultLanguage = (String) this.defaultLanguage; // LINE: 114
    cloned.defaultBaseDirection = (String) this.defaultBaseDirection; // LINE: 115
    cloned.processingMode = (String) this.processingMode; // LINE: 116
    return cloned; // LINE: 117
  }

  public Context getContext(Object contextData) {
    return this.getContext(contextData, null);
  }
  public Context getContext(Object contextData, String baseUrl) {
    return this.getContext(contextData, baseUrl, null);
  }
  public Context getContext(Object contextData, String baseUrl, Set<String> remoteContexts) {
    return this.getContext(contextData, baseUrl, remoteContexts, false);
  }
  public Context getContext(Object contextData, String baseUrl, Set<String> remoteContexts, Boolean overrideProtected) {
    return this.getContext(contextData, baseUrl, remoteContexts, overrideProtected, true);
  }
  public Context getContext(Object contextData, String baseUrl, Set<String> remoteContexts, Boolean overrideProtected, Boolean validateScoped) { // LINE: 119
    if (remoteContexts == null) { // LINE: 124
      remoteContexts = new HashSet(); // LINE: 125
    }
    Context localContext = (Context) this.copy(); // LINE: 127
    if (contextData instanceof Map) { // LINE: 129
      Object propagate = (Object) ((Map) contextData).get(PROPAGATE); // LINE: 130
      if (propagate instanceof Boolean) { // LINE: 131
        localContext.propagate = (Boolean) propagate; // LINE: 132
      }
    }
    if (localContext.previousContext == null) { // LINE: 138
      localContext.previousContext = this; // LINE: 139
    }
    localContext.readContext(contextData, baseUrl, remoteContexts, overrideProtected, validateScoped); // LINE: 141
    return localContext; // LINE: 144
  }

  protected void readContext(Object contextData, /*@Nullable*/ String baseUrl, Set<String> remoteContexts, Boolean overrideProtected, Boolean validateScoped) { // LINE: 146
    List<Object> normalizedContextData; // LINE: 153
    if (contextData instanceof List) { // LINE: 154
      normalizedContextData = (List) contextData; // LINE: 155
    } else {
      normalizedContextData = new ArrayList<>(Arrays.asList(new Object[] {(Object) contextData})); // LINE: 157
    }
    if (baseUrl == null) { // LINE: 159
      baseUrl = (String) this.baseIri; // LINE: 160
    }
    for (Object context : normalizedContextData) { // LINE: 163
      if (context == null) { // LINE: 165
        if ((overrideProtected == false && this.terms.values().stream().anyMatch(term -> term.isProtected))) { // LINE: 167
          throw new InvalidContextNullificationError(); // LINE: 169
        }
        /*@Nullable*/ Context prev = (this.propagate == false ? this.copy() : null); // LINE: 172
        this.initialize(baseUrl, baseUrl); // LINE: 173
        if (prev != null) { // LINE: 174
          this.previousContext = prev; // LINE: 175
        }
        continue; // LINE: 177
      }
      if (context instanceof String) { // LINE: 180
        this.readContextLink((String) context, baseUrl, remoteContexts, overrideProtected, validateScoped); // LINE: 181
      } else if (context instanceof Map) { // LINE: 184
        this.readContextDefinition((Map) context, baseUrl, remoteContexts, overrideProtected, validateScoped); // LINE: 185
      } else {
        throw new InvalidLocalContextError(); // LINE: 189
      }
    }
  }

  protected void readContextLink(String href, String baseUrl, Set<String> remoteContexts, Boolean overrideProtected, Boolean validateScoped) { // LINE: 191
    try { // LINE: 195
      href = resolveIri(baseUrl, href); // LINE: 196
    } catch (Exception e) { // LINE: 197
      throw new LoadingDocumentFailedError(); // LINE: 198
    }
    if ((!(validateScoped) && remoteContexts.contains(href))) { // LINE: 200
      return; // LINE: 201
    }
    if (remoteContexts.size() > MAX_REMOTE_CONTEXTS) { // LINE: 203
      throw new ContextOverflowError(); // LINE: 204
    } else {
      remoteContexts.add(href); // LINE: 206
    }
    Object contextDocument = (Object) this.loadDocument(href); // LINE: 208
    if ((!(contextDocument instanceof Map) || !((Map) contextDocument).containsKey(CONTEXT))) { // LINE: 210
      throw new InvalidRemoteContextError(); // LINE: 211
    }
    Object loaded = (Object) ((Map) contextDocument).get(CONTEXT); // LINE: 213
    this.readContext(loaded, href, new HashSet(remoteContexts), overrideProtected, validateScoped); // LINE: 216
  }

  protected Object loadDocument(String href) {
    return this.loadDocument(href, JSONLD_CONTEXT_RELATION);
  }
  protected Object loadDocument(String href, String profile) {
    return this.loadDocument(href, profile, JSONLD_CONTEXT_RELATION);
  }
  protected Object loadDocument(String href, String profile, String requestProfile) { // LINE: 224
    /* ... */; // LINE: 234
    return loadJson(href); // LINE: 236
  }

  protected void readContextDefinition(Map<String, Object> context, String baseUrl, Set<String> remoteContexts, Boolean overrideProtected, Boolean validateScoped) { // LINE: 238
    Object version = (Object) context.get(VERSION); // LINE: 244
    if (version != null) { // LINE: 245
      if ((this.processingMode == null && ((Object) JSONLD10) == null || this.processingMode != null && (this.processingMode).equals(JSONLD10))) { // LINE: 246
        throw new ProcessingModeConflictError(); // LINE: 247
      }
      if ((version instanceof Double && (version == null && ((Object) 1.1) == null || version != null && (version).equals(1.1)))) { // LINE: 248
        this.version = (Double) version; // LINE: 249
      } else {
        throw new InvalidVersionValueError(); // LINE: 251
      }
    }
    if (context.containsKey(IMPORT)) { // LINE: 254
      context = this.handleImport(context, baseUrl); // LINE: 255
    }
    if ((context.containsKey(BASE) && remoteContexts.size() == 0)) { // LINE: 258
      Object base = (Object) context.get(BASE); // LINE: 260
      if (base == null) { // LINE: 262
        this.baseIri = ""; // LINE: 264
      } else if ((this.baseIri != null && base instanceof String)) { // LINE: 267
        this.baseIri = resolveIri(this.baseIri, (String) base); // LINE: 268
      } else if ((base instanceof String && isIri((String) base))) { // LINE: 269
        this.baseIri = (String) base; // LINE: 270
      } else {
        throw new InvalidBaseIriError(); // LINE: 273
      }
    }
    if (context.containsKey(VOCAB)) { // LINE: 276
      Object vocab = (Object) context.get(VOCAB); // LINE: 278
      if (vocab == null) { // LINE: 280
        this.vocabularyMapping = null; // LINE: 281
      } else if ((vocab instanceof String && (isIriRef((String) vocab) || isBlank((String) vocab)))) { // LINE: 283
        this.vocabularyMapping = (String) this.expandDocRelativeVocabIri((String) vocab); // LINE: 284
      } else {
        throw new InvalidVocabMappingError(); // LINE: 288
      }
    }
    if (context.containsKey(LANGUAGE)) { // LINE: 291
      Object lang = (Object) context.get(LANGUAGE); // LINE: 292
      if (lang == null) { // LINE: 293
        this.defaultLanguage = null; // LINE: 294
      } else if (lang instanceof String) { // LINE: 295
        if (!(isLangTag((String) lang))) { // LINE: 296
          warning("Language tag " + lang + " in context is not well-formed"); // LINE: 297
        }
        this.defaultLanguage = ((String) lang).toLowerCase(); // LINE: 298
      } else {
        throw new InvalidDefaultLanguageError(); // LINE: 300
      }
    }
    if (context.containsKey(DIRECTION)) { // LINE: 303
      Object direction = (Object) context.get(DIRECTION); // LINE: 304
      if ((direction == null || (direction instanceof String && DIRECTIONS.contains(direction)))) { // LINE: 305
        this.defaultBaseDirection = (String) direction; // LINE: 306
      } else {
        throw new InvalidBaseDirectionError(direction.toString()); // LINE: 308
      }
    }
    if (context.containsKey(PROPAGATE)) { // LINE: 311
      Object propagate = (Object) context.get(PROPAGATE); // LINE: 312
      if ((this.processingMode == null && ((Object) JSONLD10) == null || this.processingMode != null && (this.processingMode).equals(JSONLD10))) { // LINE: 314
        throw new InvalidContextEntryError(); // LINE: 315
      }
      if (!(propagate instanceof Boolean)) { // LINE: 317
        throw new InvalidPropagateValueError(propagate.toString()); // LINE: 318
      }
    }
    Map<String, Boolean> defined = new HashMap<>(); // LINE: 323
    for (Map.Entry<String, Object> key_value : context.entrySet()) { // LINE: 326
      String key = key_value.getKey();
      Object value = key_value.getValue();
      if (CONTEXT_KEYWORDS.contains(key)) { // LINE: 327
        continue; // LINE: 328
      }
      Boolean isprotected = (Boolean) ((Boolean) context.get(PROTECTED)); // LINE: 338
      new Term(this, context, key, value, defined, baseUrl, isprotected, overrideProtected); // LINE: 339
    }
  }

  protected Map handleImport(Map<String, Object> context, String baseUrl) { // LINE: 341
    Object importValue = (Object) context.get(IMPORT); // LINE: 342
    if ((this.processingMode == null && ((Object) JSONLD10) == null || this.processingMode != null && (this.processingMode).equals(JSONLD10))) { // LINE: 344
      throw new InvalidContextEntryError(); // LINE: 345
    }
    if (!(importValue instanceof String)) { // LINE: 347
      throw new InvalidImportValueError(importValue.toString()); // LINE: 348
    }
    importValue = resolveIri(baseUrl, (String) importValue); // LINE: 350
    Object contextDocument = (Object) this.loadDocument((String) importValue); // LINE: 352
    if ((!(contextDocument instanceof Map) || !((Map) contextDocument).containsKey(CONTEXT))) { // LINE: 356
      throw new InvalidRemoteContextError(); // LINE: 357
    }
    Object importContext = (Object) ((Map) contextDocument).get(CONTEXT); // LINE: 358
    if (!(importContext instanceof Map)) { // LINE: 359
      throw new InvalidRemoteContextError(); // LINE: 360
    }
    if (((Map) importContext).containsKey(IMPORT)) { // LINE: 362
      throw new InvalidContextEntryError(); // LINE: 363
    }
    ((Map) importContext).putAll(context); // LINE: 365
    ((Map) importContext).remove(IMPORT); // LINE: 366
    return (Map) importContext; // LINE: 367
  }

  public /*@Nullable*/ String expandVocabIri(String value) { // LINE: 369
    return this.expandIri(value, null, null, false, true); // LINE: 370
  }

  public /*@Nullable*/ String expandDocRelativeIri(String value) { // LINE: 372
    return this.expandIri(value, null, null, true, false); // LINE: 373
  }

  public /*@Nullable*/ String expandDocRelativeVocabIri(String value) { // LINE: 375
    return this.expandIri(value, null, null, true, true); // LINE: 376
  }

  protected /*@Nullable*/ String expandInitVocabIri(String value, Map<String, Object> localContext, Map<String, Boolean> defined) { // LINE: 378
    return this.expandIri(value, localContext, defined, false, true); // LINE: 383
  }

  public /*@Nullable*/ String expandIri(String value) {
    return this.expandIri(value, null);
  }
  public /*@Nullable*/ String expandIri(String value, /*@Nullable*/ Map<String, Object> localContext) {
    return this.expandIri(value, localContext, null);
  }
  public /*@Nullable*/ String expandIri(String value, /*@Nullable*/ Map<String, Object> localContext, /*@Nullable*/ Map<String, Boolean> defined) {
    return this.expandIri(value, localContext, defined, false);
  }
  public /*@Nullable*/ String expandIri(String value, /*@Nullable*/ Map<String, Object> localContext, /*@Nullable*/ Map<String, Boolean> defined, Boolean docRelative) {
    return this.expandIri(value, localContext, defined, docRelative, false);
  }
  public /*@Nullable*/ String expandIri(String value, /*@Nullable*/ Map<String, Object> localContext, /*@Nullable*/ Map<String, Boolean> defined, Boolean docRelative, Boolean vocab) { // LINE: 386
    if ((KEYWORDS.contains(value) || value == null)) { // LINE: 393
      return value; // LINE: 394
    }
    if (hasKeywordForm(value)) { // LINE: 397
      warning("Id " + value + " looks like a keyword"); // LINE: 398
      return null; // LINE: 399
    }
    if ((localContext != null && localContext.containsKey(value) && defined != null && (!defined.containsKey(value) || defined.get(value) != true))) { // LINE: 402
      new Term(this, localContext, value, localContext.get(value), defined); // LINE: 403
    }
    /*@Nullable*/ Term iriTerm = (/*@Nullable*/ Term) this.terms.get(value); // LINE: 405
    if ((iriTerm != null && KEYWORDS.contains(iriTerm.iri))) { // LINE: 408
      return iriTerm.iri; // LINE: 409
    }
    if ((vocab && iriTerm != null)) { // LINE: 412
      return iriTerm.iri; // LINE: 413
    }
    if ((value.length() > 1 && value.substring(1).contains(":"))) { // LINE: 416
      Integer idx = (Integer) value.indexOf(":"); // LINE: 418
      String prefix = value.substring(0, idx); // LINE: 419
      String suffix = value.substring(idx + 1); // LINE: 420
      if (((prefix == null && ((Object) "_") == null || prefix != null && (prefix).equals("_")) || suffix.startsWith("//"))) { // LINE: 423
        return value; // LINE: 424
      }
      if ((localContext != null && localContext.containsKey(prefix) && defined != null)) { // LINE: 428
        if ((!defined.containsKey(prefix) || defined.get(prefix) != true)) { // LINE: 429
          new Term(this, localContext, prefix, localContext.get(prefix), defined); // LINE: 430
        }
      }
      /*@Nullable*/ Term pfxTerm = (/*@Nullable*/ Term) this.terms.get(prefix); // LINE: 433
      if ((pfxTerm != null && pfxTerm.iri != null && pfxTerm.isPrefix)) { // LINE: 434
        return pfxTerm.iri + suffix; // LINE: 435
      }
      if ((!(value.startsWith("#")) && isIri(value))) { // LINE: 439
        return value; // LINE: 440
      }
    }
    if ((vocab && this.vocabularyMapping != null)) { // LINE: 443
      return this.vocabularyMapping + value; // LINE: 444
    } else if (docRelative) { // LINE: 447
      return resolveIri(this.baseIri, value); // LINE: 448
    }
    return value; // LINE: 451
  }
}
