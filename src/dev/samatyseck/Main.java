package dev.samatyseck;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class Main {

    public static void main(String[] args) {
        BufferedWriter out = null;
        try {
            //Sets up output file "output.csv" and writer to file "out"
            FileWriter fstream = new FileWriter("output.csv", true);
            out = new BufferedWriter(fstream);

            //Simulates with a uniform distribution for the number of processors {2,4,8,16,32,64}
            out.write("Uniform Distribution\n");
            int i = 2;
            while (i <= 64) {
                // Writes the number of processors to the beginning of the next line.
                // The simulate function prints out average wait times separated by commas
                out.write(Integer.toString(i));
                simulate(i, out, false);
                i = i * 2;
            }

            //Simulates with a normal (Gaussian) distribution for the number of processors {2,4,8,16,32,64}
            out.write("Gaussian Distribution\n");
            i = 2;
            while (i <= 64) {
                out.write(Integer.toString(i));
                simulate(i, out, true);
                i = i * 2;
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }

    private static void simulate(int p, BufferedWriter out, boolean normal) {
        //Simulates a 'p' amount of processors using 1-2048 memory modules.
        // If 'normal' is false, it uses a uniform distibution to determine which memory to access.
        // If 'normal' is true, it uses a normal distribution to determine the memory to access with random averages for each processor.
        Random rand = new Random();

        // The processors array is the memory module number that the processor is requesting access to.
        // The value is -1 if the processor is unassigned/just got access to a module.
        // The waits array is the number of cycles each processor has waited in the current simulation round.
        // The priority array is used to determine the processor to allow access when more than one processor requests the same module.
        // The means array is used when the random numbers are normal
        // and serves as a place to store the means of the normal distribution for each processor.

        int[] processors = new int[p];
        int[] waits = new int[p];
        int[] priority = new int[p];
        int[] means = new int[p];

        int m = 1;
        while (m <= 2048) {
            for (int i = 0; i < p; i++) {
                //Initialize/Reset Arrays at the beginning of round of simulation
                //                                                      (when the amount of memory modules changes)
                processors[i] = -1;
                waits[i] = 0;
                priority[i] = 0;
                if (normal) {
                    means[i] = rand.nextInt(m);
                }
            }
            // 'Prev' stores the previous average wait time, when used in conjunction with 'n' later it,
            // it determines when a round of simulation has become 'stable'

            // 'C' is the number of iterations the current round has gone through
            // 'Stable' works with the average wait times to require a number of rounds to be 'stable' before stopping.
            //                                                         (20 when the wait time is 0 and 4 when it is not)

            double prev = 0;
            int c = 0;
            int stable = 0;

            while (true) {
                //Initially determines the average wait time of the current round,
                // stops if either of the the terms described above are met.

                double n = avgCheck(waits, p, c);
                if (n == 0 && prev == 0) {
                    stable++;
                    if (stable == 20) {
                        prev = n;
                        break;
                    }
                } else if (n != 0 && prev != 0) {
                    if ((prev / n) < 1.02 && stable < 4) {
                        stable++;
                    } else if ((prev / n) < 1.02 && stable >= 4) {
                        prev = n;
                        break;
                    } else {
                        stable = 0;
                        prev = n;
                        // Prev is only overwritten when the simulation is not stable or ends, this helps with the case where
                        // 4 consecutive values are stable but the first value differs significantly from the 4th
                    }
                } else {
                    stable = 0;
                    prev = n;
                }

                for (int i = 0; i < p; i++) { //Iterates through the processors to determine which ones are waiting
                    if (processors[i] == -1) { //If the processor is unassigned, have it request a new processor
                        if (!normal) {
                            processors[i] = rand.nextInt(m);
                        } else {
                            processors[i] = Math.round(Math.round(rand.nextGaussian() * Math.sqrt((m / Math.pow(p, 2))) + means[i]));
                            if (processors[i] < 0) {
                                processors[i] = -processors[i];
                            }
                        }
                    } else {
                        // If the processor is still assigned then it is waiting,
                        // so increase the wait counter for it and reduce the priority so it will be chosen over
                        // any processor that requests the same memory module before this processor gets access.
                        priority[i]--;
                        waits[i]++;
                    }
                }
                for (int i = 0; i < m; i++) {
                    // Iterates through the memory modules to determine which processor gets access to it
                    int proc = getNextProcessor(i, p, processors, priority);
                    if (proc != -1) {
                        // If a processor is found that wants access to it, give access to it and reset it's priority.
                        processors[proc] = -1;
                        priority[proc] = p;
                    }
                }
                c++;
            }
            try {
                // Writes the determined average wait time for 'p' processors and 'm' memory modules to the output file.
                out.write(',' + ((Double) prev).toString());
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
            m++;
        }
        try {
            // Writes the determined average wait time for 'p' processors and 'm' memory modules to the output file.
            out.write('\n');
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static double avgCheck(int[] waits, int p, int c) {
        // Simple function that calculates the current average wait time by summing up the average for each processor
        // and dividing that number by the amount of processors.
        double totalWait = 0;
        if (c == 0) {
            return 0;
        }
        for (int i = 0; i < p; i++) {
            totalWait = totalWait + ((double) waits[i] / (double) c);
        }

        return totalWait / p;
    }

    private static int getNextProcessor(int module, int p, int[] processors, int[] priority) {
        // Determines which processor gets access to the memory module 'm', if none want it return -1
        // else return the processor with the lowest priority number, lower processor numbers win when both processors
        // have the same priority.
        int min_proc = -1;
        int min_priority = p + 1;
        for (int proc = 0; proc < p; proc++) {
            if (processors[proc] == module && priority[proc] < min_priority) {
                min_proc = proc;
                min_priority = priority[proc];
            }
        }
        return min_proc;
    }
}

