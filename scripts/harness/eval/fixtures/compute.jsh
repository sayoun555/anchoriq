// EVAL v3 fixture — 컴파일 멀쩡, 출력이 비자명한 20스텝 혼합 연산.
// 읽기(암산)로는 불안정, 실행(jshell)으로는 확정. grounding이 reading을 이기는지 테스트.
int compute(int n) {
    int r = 0;
    for (int i = 1; i <= n; i++) {
        if (i % 3 == 0)      r += i * 2;
        else if (i % 5 == 0) r += i;
        else                 r -= i;
    }
    return r;
}
System.out.println("compute(20)=" + compute(20));
