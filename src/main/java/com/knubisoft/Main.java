package com.knubisoft;

import com.knubisoft.service.SearcherImageDiff;

public class Main {
    private static final String FILE_A = "src/main/resources/test1.jpg";
    private static final String FILE_B = "src/main/resources/test2.jpg";

    public static void main(String[] args) {
        SearcherImageDiff searcherImageDiff = new SearcherImageDiff();
        searcherImageDiff.launchFinderImageDiff(FILE_A, FILE_B);
    }
}
