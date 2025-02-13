/**
 * This file was automatically generated by the TRLD transpiler.
 * Source: trld/trig/parser.py
 */
package trld.trig;

//import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.*;

import trld.Builtins;
import trld.KeyValue;

import trld.Input;
import static trld.Common.dumpJson;
import static trld.jsonld.Base.VALUE;
import static trld.jsonld.Base.TYPE;
import static trld.jsonld.Base.LANGUAGE;
import static trld.jsonld.Base.ID;
import static trld.jsonld.Base.LIST;
import static trld.jsonld.Base.GRAPH;
import static trld.jsonld.Base.CONTEXT;
import static trld.jsonld.Base.VOCAB;
import static trld.jsonld.Base.BASE;
import static trld.jsonld.Base.PREFIX;
import static trld.jsonld.Base.PREFIX_DELIMS;
import static trld.Rdfterms.RDF_TYPE;
import static trld.Rdfterms.XSD;
import static trld.Rdfterms.XSD_DOUBLE;
import static trld.Rdfterms.XSD_INTEGER;
import static trld.trig.Parser.*;


public class ReadIRI extends ReadTerm { // LINE: 195
  ReadIRI(/*@Nullable*/ ParserState parent) { super(parent); };
  public static final Pattern MATCH = (Pattern) Pattern.compile("\\S"); // LINE: 197

  public void init() { // LINE: 199
    this.escapeChars = new HashMap<>(); // LINE: 200
  }

  public boolean accept(String c) { // LINE: 202
    return (this.MATCH.matcher(c).matches() ? c : null) != null; // LINE: 203
  }

  public Map.Entry<ParserState, Object> consume(String c, Object prevValue) { // LINE: 205
    if ((c == null && ((Object) ">") == null || c != null && (c).equals(">"))) { // LINE: 206
      String value = (String) this.pop(); // LINE: 207
      return new KeyValue(this.parent, Builtins.mapOf(ID, value)); // LINE: 208
    } else if (this.handleEscape(c)) { // LINE: 209
      return new KeyValue(this, null); // LINE: 210
    } else {
      if (!(this.accept(c))) { // LINE: 212
        throw new NotationError("Invalid URI character: " + c); // LINE: 213
      }
      this.collect(c); // LINE: 214
      return new KeyValue(this, null); // LINE: 215
    }
  }
}
