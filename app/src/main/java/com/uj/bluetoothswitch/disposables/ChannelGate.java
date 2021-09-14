package com.uj.bluetoothswitch.disposables;


import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.IntStream;


public class ChannelGate {

    private Map<Byte[], Integer> patterns;
    //Размер самого большо паттерна в мапе
    private int procArraySize;
    private boolean singlePatternMode = false;

    public ChannelGate(Map<Byte[], Integer> codePatternPair) {

        this.patterns = new HashMap<>(codePatternPair);
        if (patterns.isEmpty()) throw new RuntimeException("Map most have some elements");
        procArraySize = patterns.keySet().stream().map(ar -> ar.length).reduce(0, ((prev, now) -> prev > now ? prev : now));
    }

    public static ChannelGate getSingleModeChannelGate(Byte[] pattern) {
        Map<Byte[], Integer> codePatternPair = new HashMap<>();
        codePatternPair.put(pattern, 1);
        ChannelGate result = new ChannelGate(codePatternPair);
        result.setSingleMode(true);
        return result;
    }

    private void setSingleMode(boolean b) {
        this.singlePatternMode = b;
    }


    private Boolean matchToPattern(Byte[] inpAr, Byte[] pattern) {
        int patternSize = pattern.length;
        return IntStream.range(0, patternSize).allMatch(i -> {
            return pattern[i] == inpAr[i];
        });


    }


    private Byte[] nextIteration(Byte[] procesAr, InputStream inputStream) {
        for (int i = 0; i < procArraySize - 1; i++) {
            procesAr[i] = procesAr[i + 1];
        }
        try {
            procesAr[procArraySize - 1] = (byte) inputStream.read();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return procesAr;
    }

    /**
     * Ввращает код совпавшего паттерна.
     */

    public int getMatchCode(InputStream inputStream) throws IOException {
        Integer matchCode = null;
        Byte[] processArr = new Byte[procArraySize];


        for (int i = 0; i < procArraySize; i++) {
            processArr[i] = (Byte) (byte) inputStream.read();
            if (processArr[i] == -1)
                throw new RuntimeException("Can't read enough bytes from inputstream!");
        }

        while (true) {

            final Byte[] finalProcAr = Arrays.copyOf(processArr, processArr.length);
            int answer = -1;

            answer = patterns.keySet().stream()
                    .mapToInt(pattern -> {
                        if (matchToPattern(finalProcAr, pattern)) {
                            return patterns.get(pattern);
                        } else
                            return new Integer(-1);
                    })
                    .findFirst()
                    .getAsInt();


            if (answer == -1) {
                processArr = nextIteration(processArr, inputStream);
                continue;
            } else {
                matchCode = answer;
                break;
            }


        }
        return matchCode;


    }

    public InputStream singleMatch(InputStream inputStream) throws IOException
            , UnsupportedOperationException {
        if (this.singlePatternMode == false)
            throw new UnsupportedOperationException("This gate is not in single Pattern Mode");
        getMatchCode(inputStream);
        return inputStream;
    }


}

