package whelk.search

import spock.lang.Specification
import whelk.JsonLd

class ESQuerySpec extends Specification {
    ESQuery es
    void setup() {
        es = new ESQuery()
        es.setKeywords(['bar'] as Set)

        Map context = ["@context": ["langAliasedByLang": ["@id": "langAliased", "@container": "@language"]]]
        Map display = [:]
        Map vocab = [:]
        es.jsonld = new JsonLd(context, display, vocab)
    }

    def "should get query string"() {
        expect:
        es.getQueryString(params) == query
        where:
        params                          | query
        ['foo': ['bar']]                | '*'
        ['q': ['abcd'], 'foo': ['bar']] | 'abcd'
    }

    def "should get pagination params"() {
        expect:
        es.getPaginationParams(params) == result
        where:
        params                                        | result
        // Default limit is 50
        ['foo': ['bar']]                              | new Tuple2(50, 0)
        ['_limit': ['100']]                           | new Tuple2(100, 0)
        ['_offset': ['10']]                           | new Tuple2(50, 10)
        ['_limit': ['100', '200'], '_offset': ['10']] | new Tuple2(100, 10)
    }

    def "should get sort clauses"() {
        expect:
        es.getSortClauses(params) == result
        where:
        params                  | result
        [:]                     | null
        ['_sort': []]           | null
        ['_sort': ['']]         | null
        // `bar` has a keyword field in the mappings
        ['_sort': ['foo,-bar']] | [['foo': ['order': 'asc']], ['bar.keyword': ['order': 'desc']]]
        ['_sort': ['hasTitle.mainTitle']] | [['hasTitle.mainTitle': ['order': 'asc',
                                                                     'nested_path': 'hasTitle',
                                                                     'nested_filter': ['term': ['hasTitle.@type': 'Title']]]]]
    }

    def "should get site filter"() {
        expect:
        es.getSiteFilter(params) == result
        where:
        params                                          | result
        [:]                                             | null
        ['_site_base_uri': []]                          | null
        ['_site_base_uri': ['https://foo.example.com']] | [['bool':
                                                               ['should': [
                                                                   ['prefix': ['@id': 'https://foo.example.com']],
                                                                   ['prefix': ['sameAs.@id': 'https://foo.example.com']]
                                                               ]]
                                                          ]]

    }

    def "should get filters"(Map<String, String[]> params, List result) {
        expect:
        es.getFilters(params) == result
        where:
        params                  | result
        [:]                     | null
        ['foo': ['bar', 'baz']] | [['bool': ['must': [
                                                ['bool': [
                                                    'should': [
                                                        ['simple_query_string': ['query': 'bar',
                                                                                 'fields': ['foo'],
                                                                                 'default_operator': 'AND']],
                                                        ['simple_query_string': ['query': 'baz',
                                                                                 'fields': ['foo'],
                                                                                 'default_operator': 'AND']]
                                                    ]
                                                ]]
                                            ]]
                                  ]]

        ['foo': ['*bar', 'baz']] | [['bool': ['must': [
                                                ['bool': [
                                                    'should': [
                                                        ['query_string'        : ['query': '*bar',
                                                                                 'fields': ['foo'],
                                                                                 'default_operator': 'AND']],
                                                        ['simple_query_string' : ['query': 'baz',
                                                                                 'fields': ['foo'],
                                                                                 'default_operator': 'AND']]
                                                    ]
                                                ]]
                                            ]]
                                   ]]

        ['and-foo': ['bar', 'baz']]     | [['bool': ['must': [
                                                        ['bool': [
                                                            'should': [
                                                                ['simple_query_string': ['query': 'bar',
                                                                                         'fields': ['foo'],
                                                                                         'default_operator': 'AND']],
                                                            ]
                                                        ]],
                                                        ['bool': [
                                                            'should': [
                                                                ['simple_query_string': ['query': 'baz',
                                                                                         'fields': ['foo'],
                                                                                         'default_operator': 'AND']]
                                                            ]
                                                        ]]
                                                ]]
                                          ]]


        ['foo': ['bar', 'baz'],
         'not-foo': ['zzz']]     | [['bool': ['must': [
                                                ['bool': [
                                                    'should': [
                                                        ['simple_query_string': ['query': 'bar',
                                                                                 'fields': ['foo'],
                                                                                 'default_operator': 'AND']],
                                                        ['simple_query_string': ['query': 'baz',
                                                                                 'fields': ['foo'],
                                                                                 'default_operator': 'AND']]
                                                    ]
                                                ]]
                                            ],
                                            'must_not': [
                                                ['bool': [
                                                    'should': [
                                                        ['simple_query_string' : ['query': 'zzz',
                                                                                 'fields': ['foo'],
                                                                                 'default_operator': 'AND']]
                                                    ]
                                                ]]
                                            ]]
                                  ]]


        ['langAliased': ['baz']] | [['bool': ['must': [
                                                ['bool': [
                                                    'should': [
                                                        ['simple_query_string' : ['query': 'baz',
                                                                                  'fields': ['__langAliased'],
                                                                                  'default_operator': 'AND']]]]]]]]]
    }

    def "should create bool filter"(String key, String[] vals, Map result) {
        expect:
        es.createBoolFilter([(key): vals]) == result
        where:
        key   | vals           | result
        'foo' | ['bar', 'baz'] | ['bool': ['should': [
                                              ['simple_query_string': ['query': 'bar',
                                                                       'fields': ['foo'],
                                                                       'default_operator': 'AND']],
                                              ['simple_query_string': ['query': 'baz',
                                                                       'fields': ['foo'],
                                                                       'default_operator': 'AND']]]]]
        'foo' | ['bar', '*baz'] | ['bool': ['should': [
                                              ['simple_query_string': ['query': 'bar',
                                                                       'fields': ['foo'],
                                                                       'default_operator': 'AND']],
                                              ['query_string'       : ['query': '*baz',
                                                                       'fields': ['foo'],
                                                                       'default_operator': 'AND']]]]]
    }

    def "should get agg query"() {
        when:
        Map emptyAggs = [:]
        Map emptyAggsResult = [(JsonLd.TYPE_KEY): ['terms': ['field': JsonLd.TYPE_KEY]]]
        Map simpleAggs = ['_statsrepr': ['{"foo": {"sort": "key", "sortOrder": "desc", "size": 5}}']]
        Map simpleAggsResult = ['foo': ['terms': ['field': 'foo', 'size': 5, 'order': ['_key': 'desc']]]]
        Map subAggs = ['_statsrepr': ['{"bar": {"subItems": {"foo": {"sort": "key"}}}}']]
        // `bar` has a keyword field in the mappings
        Map subAggsResult = ['bar.keyword': ['terms': ['field': 'bar.keyword',
                                               'size': 10,
                                               'order': ['_count': 'desc']],
                                     'aggs': [
                                        'foo': ['terms': ['field': 'foo',
                                                          'size': 10,
                                                          'order': ['_key': 'desc']]]
                                     ]]]

        then:
        es.getAggQuery(emptyAggs) == emptyAggsResult
        es.getAggQuery(simpleAggs) == simpleAggsResult
        es.getAggQuery(subAggs) == subAggsResult
    }

    def "should get keyword fields"() {
        when:
        Map emptyMappings = [:]
        Set emptyResult = [] as Set
        Map simpleMappings = [
            'properties': [
                'foo': [
                    'type': 'text',
                    'fields': [
                        'keyword': [
                            'type': 'keyword'
                        ]
                    ]
                ]
            ]
        ]
        Set simpleResult = ['foo'] as Set
        Map nestedMappings = [
            'properties': [
                'foo': [
                    'type': 'text',
                    'fields': [
                        'keyword': [
                            'type': 'keyword'
                        ]
                    ],
                    'properties': [
                        '@type': [
                            'type': 'keyword'
                        ],
                        'bar': [
                            'properties': [
                                'baz': [
                                    'type': 'text',
                                    'fields': [
                                        'keyword': [
                                            'type': 'keyword'
                                        ]
                                    ]
                                ],
                                'quux': [
                                    'type': 'keyword'
                                ]
                            ]
                        ]
                    ]
                ],
                'baz': [
                    'type': 'keyword'
                ]
            ]
        ]
        Set nestedResult = ['foo', 'foo.bar.baz'] as Set
        then:
        es.getKeywordFields(emptyMappings) == emptyResult
        es.getKeywordFields(simpleMappings) == simpleResult
        es.getKeywordFields(nestedMappings) == nestedResult
    }

    def "should expand @type param"() {
        when:
        Map context = [:]
        Map display = [:]
        Map vocab = ['@graph': [['@id': 'foo', '@type': 'Class'],
                                ['@id': 'bar', '@type': 'Class', 'subClassOf': [['@id': 'foo']]],
                                ['@id': 'baz', '@type': 'Class', 'subClassOf': [['@id': 'bar']]]]]
        JsonLd jsonld = new JsonLd(context, display, vocab)

        then:
        ESQuery.expandTypeParam(null, jsonld) == [] as String[]
        ESQuery.expandTypeParam(['bar'] as String[], jsonld) == ['baz', 'bar'] as String[]
        ESQuery.expandTypeParam(['foo', 'baz'] as String[], jsonld) == ['baz'] as String[]
    }

    def "should hide keyword fields in ES response"() {
        when:
        Map emptyEsResponse = [:]
        Map emptyExpected = [:]
        Map esResponse = ['foo': ['bar.keyword.baz': 1,
                                  'bar.keyword': 2],
                          'aggregations': ['baz': 3,
                                           'bar.keyword': 4,
                                           'foo.keyword.quux': 5]]
        Map expected = ['foo': ['bar.keyword.baz': 1,
                                'bar.keyword': 2],
                        'aggregations': ['baz': 3,
                                         'bar': 4,
                                         'foo.keyword.quux': 5]]

        then:
        emptyExpected == es.hideKeywordFields(emptyEsResponse)
        expected == es.hideKeywordFields(esResponse)
    }
    
    def "should recognize leading wildcard queries"() {
        expect:
        ESQuery.isSimple(query) == result

        where:
        query       | result
        "*"         | true
        "*   "      | true
        "*foo"      | false
        "foo *bar"  | false
        "foo* bar"  | true
        "foo* *bar" | false
    }

    def "should escape queries as needed"() {
        expect:
        ESQuery.escapeNonSimpleQueryString(query) == result

        where:
        query                         | result
        "([foo] | {bar}) | foo & bar" | "(\\[foo\\] | \\{bar\\}) | foo \\& bar"
        "-not-this -foo--bar--baz-"   | "-not\\-this -foo\\-\\-bar\\-\\-baz\\-"
    }
}
