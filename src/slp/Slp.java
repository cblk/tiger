package slp;


class Slp {
    // expression
    static class Exp {
        // base class
        static abstract class T {
        }

        // id
        static class Id extends T {
            final String id;

            Id(String id) {
                this.id = id;
            }
        }

        // id
        static class Num extends T {
            final int num;

            Num(int num) {
                this.num = num;
            }
        }

        // op
        public enum OP_T {
            ADD, SUB, TIMES, DIVIDE
        }

        static class Op extends T {
            final OP_T op;
            final T left;
            final T right;

            Op(OP_T op, T left, T right) {
                this.op = op;
                this.left = left;
                this.right = right;
            }
        }

        // Eseq
        static class Eseq extends T {
            final Stm.T stm;
            final T exp;

            Eseq(Stm.T stm, T exp) {
                this.stm = stm;
                this.exp = exp;
            }
        }
    }// end of expression

    // explist
    static class ExpList {
        // base class
        static abstract class T {
        }

        // pair
        static class Pair extends T {
            final Exp.T exp;
            final ExpList.T list;

            Pair(Exp.T exp, T list) {
                super();
                this.exp = exp;
                this.list = list;
            }
        }

        // last
        static class Last extends T {
            final Exp.T exp;

            Last(Exp.T exp) {
                super();
                this.exp = exp;
            }
        }
    }// end of explist

    // statement
    public static class Stm {
        // base class
        static abstract class T {
        }

        // Compound (s1, s2)
        static class Compound extends T {
            final T s1;
            final T s2;

            Compound(T s1, T s2) {
                this.s1 = s1;
                this.s2 = s2;
            }
        }

        // x := e
        static class Assign extends T {
            final Exp.Id id;
            final Exp.T exp;

            Assign(Exp.Id id, Exp.T exp) {
                this.id = id;
                this.exp = exp;
            }
        }

        // print (explist)
        public static class Print extends T {
            final ExpList.T explist;

            public Print(ExpList.T explist) {
                this.explist = explist;
            }
        }

    }// end of statement

}
