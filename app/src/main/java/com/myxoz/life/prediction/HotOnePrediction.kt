package com.myxoz.life.prediction

import java.io.DataInputStream
import java.io.DataOutputStream

class HotOnePrediction(
    val buckets: Int,
    val features: Int,
    val learningRate: Double = 0.01
) {
    private val weights = Array(buckets) { DoubleArray(features) { Math.random() * 0.01 } }
    private val b = DoubleArray(buckets)

    fun predictProbFor(x: DoubleArray): DoubleArray {
        val logits = DoubleArray(buckets)
        for (c in 0 until buckets) {
            var sum = b[c]
            for (i in x.indices) {
                sum += weights[c][i] * x[i]
            }
            logits[c] = sum
        }
        return softmax(logits)
    }

    fun predict(x: DoubleArray): Int {
        val probs = predictProbFor(x)
        return probs.indices.maxByOrNull { probs[it] }!!
    }

    fun train(x: DoubleArray, trueClass: Int) {
        val probs = predictProbFor(x)
        for (c in 0 until buckets) {
            val error = probs[c] - if (c == trueClass) 1.0 else 0.0
            for (i in x.indices) {
                weights[c][i] -= learningRate * error * x[i]
            }
            b[c] -= learningRate * error
        }
    }

    private fun softmax(logits: DoubleArray): DoubleArray {
        val result = DoubleArray(buckets)
        val max = logits.maxOrNull()!!
        var sum = 0.0

        for (i in logits.indices) {
            val e = kotlin.math.exp(logits[i] - max)
            result[i] = e
            sum += e
        }

        for (i in result.indices) {
            result[i] /= sum
        }

        return result
    }


    fun  saveModelToFile(out: DataOutputStream) {
        out.writeInt(buckets)
        out.writeInt(features)

        for (c in 0 until buckets) {
            for (i in 0 until features) {
                out.writeDouble(weights[c][i])
            }
        }

        for (c in 0 until buckets) {
            out.writeDouble(b[c])
        }
    }
    companion object {
        fun loadModelFromFile(input: DataInputStream): HotOnePrediction {
            val buckets = input.readInt()
            val features = input.readInt()

            val model = HotOnePrediction(buckets, features)

            for (c in 0 until buckets) {
                for (i in 0 until features) {
                    model.weights[c][i] = input.readDouble()
                }
            }

            for (c in 0 until buckets) {
                model.b[c] = input.readDouble()
            }

            return model
        }
    }
}