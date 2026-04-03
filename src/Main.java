public class Main {

    static final int BAR_LENGTH = 30;
    static final int DELAY_MS = 100;

    public static void main(String[] args) {
        int threadCount = 5;
        int calculationLength = 50;

        System.out.println("Потоков: " + threadCount + ", шагов: " + calculationLength);
        System.out.println("----------------------------------------");

        int[] steps = new int[threadCount];
        long[] threadIds = new long[threadCount];
        boolean[] finished = new boolean[threadCount];
        long[] durations = new long[threadCount];

        Thread[] threads = new Thread[threadCount];

        // Запуск потоков с "вычислениями"
        for (int i = 0; i < threadCount; i++) {
            final int threadNumber = i;
            threads[i] = new Thread(() -> {
                long threadId = Thread.currentThread().threadId();
                threadIds[threadNumber] = threadId;
                long startTime = System.currentTimeMillis();

                for (int step = 1; step <= calculationLength; step++) {
                    try {
                        Thread.sleep(DELAY_MS);
                    } catch (InterruptedException e) {
                        break;
                    }
                    steps[threadNumber] = step;
                }

                durations[threadNumber] = System.currentTimeMillis() - startTime;
                finished[threadNumber] = true;
            });
            threads[i].start();
        }

        // Поток для отрисовки
        Thread renderThread = new Thread(() -> {
            while (true) {
                // Типа очищаем экран
                for (int i = 0; i < 30; i++) {
                    System.out.println();
                }

                System.out.println("Потоков: " + threadCount + ", шагов: " + calculationLength);
                System.out.println("----------------------------------------");

                // Вывод
                for (int i = 0; i < threadCount; i++) {
                    int currentStep = steps[i];
                    int filled = currentStep * BAR_LENGTH / calculationLength;

                    StringBuilder bar = new StringBuilder();
                    for (int j = 0; j < BAR_LENGTH; j++) {
                        bar.append(j < filled ? "#" : "-");
                    }

                    if (finished[i]) {
                        System.out.printf("Поток %-2d | ID: %-15d | [%s] %d/%d | Время: %.2f сек%n",
                                i + 1, threadIds[i], bar.toString(), calculationLength, calculationLength, durations[i] / 1000.0);
                    } else if (threadIds[i] == 0) {
                        System.out.printf("Поток %-2d | ID: %-15s | [%s] %d/%d | Время: расчёт...%n",
                                i + 1, "ожидание", bar.toString(), currentStep, calculationLength);
                    } else {
                        System.out.printf("Поток %-2d | ID: %-15d | [%s] %d/%d | Время: расчёт...%n",
                                i + 1, threadIds[i], bar.toString(), currentStep, calculationLength);
                    }
                }
                System.out.println("----------------------------------------");

                // Проверка, все ли потоки завершили работу
                boolean allFinished = true;
                for (int i = 0; i < threadCount; i++) {
                    if (!finished[i]) {
                        allFinished = false;
                        break;
                    }
                }
                if (allFinished) {
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        renderThread.start();

        try {
            for (int i = 0; i < threadCount; i++) {
                threads[i].join();
            }
            renderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}