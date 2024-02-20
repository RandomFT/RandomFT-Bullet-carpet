package carpet.prometheus.helpers.client;

import java.util.*;

public class SampleNameFilter implements Predicate<String> {
    public static final Predicate<String> ALLOW_ALL = new AllowAll();
    private final Collection<String> nameIsEqualTo;
    private final Collection<String> nameIsNotEqualTo;
    private final Collection<String> nameStartsWith;
    private final Collection<String> nameDoesNotStartWith;

    public boolean test(String sampleName) {
        return this.matchesNameEqualTo(sampleName) && !this.matchesNameNotEqualTo(sampleName) && this.matchesNameStartsWith(sampleName) && !this.matchesNameDoesNotStartWith(sampleName);
    }

    public Predicate<String> and(final Predicate<? super String> other) {
        if (other == null) {
            throw new NullPointerException();
        } else {
            return new Predicate<String>() {
                public boolean test(String s) {
                    return SampleNameFilter.this.test(s) && other.test(s);
                }
            };
        }
    }

    private boolean matchesNameEqualTo(String metricName) {
        return this.nameIsEqualTo.isEmpty() ? true : this.nameIsEqualTo.contains(metricName);
    }

    private boolean matchesNameNotEqualTo(String metricName) {
        return this.nameIsNotEqualTo.isEmpty() ? false : this.nameIsNotEqualTo.contains(metricName);
    }

    private boolean matchesNameStartsWith(String metricName) {
        if (this.nameStartsWith.isEmpty()) {
            return true;
        } else {
            Iterator var2 = this.nameStartsWith.iterator();

            String prefix;
            do {
                if (!var2.hasNext()) {
                    return false;
                }

                prefix = (String)var2.next();
            } while(!metricName.startsWith(prefix));

            return true;
        }
    }

    private boolean matchesNameDoesNotStartWith(String metricName) {
        if (this.nameDoesNotStartWith.isEmpty()) {
            return false;
        } else {
            Iterator var2 = this.nameDoesNotStartWith.iterator();

            String prefix;
            do {
                if (!var2.hasNext()) {
                    return false;
                }

                prefix = (String)var2.next();
            } while(!metricName.startsWith(prefix));

            return true;
        }
    }

    private SampleNameFilter(Collection<String> nameIsEqualTo, Collection<String> nameIsNotEqualTo, Collection<String> nameStartsWith, Collection<String> nameDoesNotStartWith) {
        this.nameIsEqualTo = Collections.unmodifiableCollection(nameIsEqualTo);
        this.nameIsNotEqualTo = Collections.unmodifiableCollection(nameIsNotEqualTo);
        this.nameStartsWith = Collections.unmodifiableCollection(nameStartsWith);
        this.nameDoesNotStartWith = Collections.unmodifiableCollection(nameDoesNotStartWith);
    }

    public static List<String> stringToList(String s) {
        List<String> result = new ArrayList();
        if (s != null) {
            StringTokenizer tokenizer = new StringTokenizer(s, ",; \t\n");

            while(tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                token = token.trim();
                if (token.length() > 0) {
                    result.add(token);
                }
            }
        }

        return result;
    }

    public static Predicate<String> restrictToNamesEqualTo(Predicate<String> filter, Collection<String> allowedNames) {
        if (allowedNames != null && !allowedNames.isEmpty()) {
            SampleNameFilter allowedNamesFilter = (new Builder()).nameMustBeEqualTo(allowedNames).build();
            return (Predicate)(filter == null ? allowedNamesFilter : allowedNamesFilter.and(filter));
        } else {
            return filter;
        }
    }

    private static class AllowAll implements Predicate<String> {
        private AllowAll() {
        }

        public boolean test(String s) {
            return true;
        }
    }

    public static class Builder {
        private final Collection<String> nameEqualTo = new ArrayList();
        private final Collection<String> nameNotEqualTo = new ArrayList();
        private final Collection<String> nameStartsWith = new ArrayList();
        private final Collection<String> nameDoesNotStartWith = new ArrayList();

        public Builder() {
        }

        public Builder nameMustBeEqualTo(String... names) {
            return this.nameMustBeEqualTo((Collection)Arrays.asList(names));
        }

        public Builder nameMustBeEqualTo(Collection<String> names) {
            this.nameEqualTo.addAll(names);
            return this;
        }

        public Builder nameMustNotBeEqualTo(String... names) {
            return this.nameMustNotBeEqualTo((Collection)Arrays.asList(names));
        }

        public Builder nameMustNotBeEqualTo(Collection<String> names) {
            this.nameNotEqualTo.addAll(names);
            return this;
        }

        public Builder nameMustStartWith(String... prefixes) {
            return this.nameMustStartWith((Collection)Arrays.asList(prefixes));
        }

        public Builder nameMustStartWith(Collection<String> prefixes) {
            this.nameStartsWith.addAll(prefixes);
            return this;
        }

        public Builder nameMustNotStartWith(String... prefixes) {
            return this.nameMustNotStartWith((Collection)Arrays.asList(prefixes));
        }

        public Builder nameMustNotStartWith(Collection<String> prefixes) {
            this.nameDoesNotStartWith.addAll(prefixes);
            return this;
        }

        public SampleNameFilter build() {
            return new SampleNameFilter(this.nameEqualTo, this.nameNotEqualTo, this.nameStartsWith, this.nameDoesNotStartWith);
        }
    }
}
