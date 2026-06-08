package oth.shipeditor.utility.objects;

import lombok.Getter;

@Getter
public class Pair<A, B> {
    private final A first;
    private final B second;

    @SuppressWarnings("WeakerAccess")
    public Pair(A firstInput, B secondInput) {
        this.first = firstInput;
        this.second = secondInput;
    }

}
