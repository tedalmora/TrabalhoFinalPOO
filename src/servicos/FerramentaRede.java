package servicos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
encapsula ping e traeroute
identifica linux e windows
*/
public final class FerramentaRede {

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final Charset CHARSET = Charset.defaultCharset(); // windows usa cp850, linux usa utf-8. Charset.defaultCharset() pega o charset do SO.

    private FerramentaRede() { }

    // ping ou mtr simples, receb e ip e vezes
    public static ResultadoPing ping(String host, int quantidade) {
        List<String> cmd = new ArrayList<>(); //comando a ser executado no SO
        cmd.add("ping");
        if (WINDOWS) {
            cmd.add("-n"); cmd.add(String.valueOf(quantidade)); //-n pra repeiticao e valor
            cmd.add("-w"); cmd.add("1500"); //-w pra timeout de 1,5s
        } else {
            cmd.add("-c"); cmd.add(String.valueOf(quantidade)); //-c pra repeiticao e valor
            cmd.add("-W"); cmd.add("2"); //-W pra timeout de 2s (linux usa segundos, windows usa milissegundos)
        }
        cmd.add(host); //coloca o ip

        List<String> linhas = executar(cmd, 15); // manda executar e pega a saida

        // Aceita "time=12.3 ms", "time<1ms", "tempo=12ms".
        Pattern patLat = Pattern.compile("(?:time|tempo)[=<]([0-9]+(?:[.,][0-9]+)?)", Pattern.CASE_INSENSITIVE); //funcao regex pra pegar a latencia. aceita time=12.3ms, time<1ms, tempo=12ms, etc
        Pattern patPerda = Pattern.compile("([0-9]+)% (?:packet )?(?:loss|perda|perdidos)", Pattern.CASE_INSENSITIVE); //funcao regex pra pegar a perda de pacotes. aceita 0% loss, 0% packet loss, 0% perda, 0% pacotes perdidos, etc

        double soma = 0;
        int respostas = 0;
        double perda = -1;

        // percorre cada linha da saida do ping e tenta extrair a latencia e a perda de pacotes usando regex
        // eu somo as latencias de cada resposta e conto quantas respostas obtive, pra depois calcular a media
        for (String l : linhas) { //pra cada linha do output
            Matcher m = patLat.matcher(l); //matcher pra latencia
            if (m.find()) { // se m tem algum resultado, ou seja, se encontrou a latencia na linha
                try {
                    // parseia a latencia e soma no total, e conta como resposta
                    soma += Double.parseDouble(m.group(1).replace(',', '.')); //troco a virgula por ponto, pq o parseDouble espera ponto como separador decimal
                    respostas++;
                } catch (NumberFormatException ignored) { } // ignora se nao conseguir parsear
            }

            Matcher mp = patPerda.matcher(l); //dou parse na perda
            if (mp.find()) { // se achei
                try { 
                    perda = Double.parseDouble(mp.group(1)); // coloco em perda
                }
                catch (NumberFormatException ignored) { }
            }
        }

        // Fallback se o parser de % não pegou.
        if (perda < 0) {
            perda = ((quantidade - respostas) * 100.0) / Math.max(1, quantidade);
        }

        boolean alcancavel = respostas > 0; // se o host responde
        double media;
        if(alcancavel) {
            media = soma / respostas; // calcula a media da latencia
        } else {
            media = -1.0; // se nao respondeu, latencia = -1
        }

        return new ResultadoPing(alcancavel, media, perda);
    }

    // Traceroute
    public static List<String> traceroute(String host) {
        List<String> cmd = new ArrayList<>();
        if (WINDOWS) {
            cmd.add("tracert"); cmd.add("-d"); cmd.add("-h"); cmd.add("20"); // -d pra nao resolver nomes, -h pra limitar a 20 hops
        } else { //linux
            cmd.add("traceroute"); cmd.add("-n"); cmd.add("-m"); cmd.add("20"); // -n pra nao resolver nomes, -m pra limitar a 20 hops
        }
        cmd.add(host);

        List<String> bruto = executar(cmd, 40); //manda executar
        List<String> hops = new ArrayList<>();
        Pattern patHop = Pattern.compile("^\\s*\\d+\\s+.*"); //regex pra pegar as linhas do traceroute que começam com um numero (hop) seguido de espacos e depois o resto da linha. ignora linhas de cabeçalho e rodapé
        for (String linha : bruto) { //pra cada linha na resposta
            if (patHop.matcher(linha).matches())  //ve se da match com regex
                hops.add(linha.trim()); //adiciona seme spaços em branco
        }
        return hops;
    }

    // roda os comandos do SO
    private static List<String> executar(List<String> cmd, long timeoutSeg) {
        List<String> linhas = new ArrayList<>(); //output do comando, linha por linha
        Process proc = null; // Process é o tipo que representa o processo do SO que vai rodar o comando. Inicialmente é null, pq ainda não foi criado.
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd); //crio um processo com o cmd
            pb.redirectErrorStream(true); // redireciona o erro para o output, pra nao perder nada
            proc = pb.start(); //start() inicia o processo e retorna um Process que representa o processo do SO que está rodando o comando. A partir daqui posso ler a saída do comando.
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), CHARSET))) { //leio um bufferedReader da saida do processo, usando o charset do SO. Uso try-with-resources pra fechar automaticamente o BufferedReader no final do bloco.
                String l;
                while ((l = br.readLine()) != null) //pra cada linha da saida do comando, leio ate o final (null)
                    linhas.add(l); //adiciono a linha na lista de linhas
            }
            if (!proc.waitFor(timeoutSeg, TimeUnit.SECONDS)) { // espera o processo terminar, com timeout. Se nao terminar, retorna false
                proc.destroyForcibly(); // se nao terminou, mata o processo forçadamente
            }
        } catch (Exception e) {
            return Collections.emptyList(); // se der algum erro, devolvo lista vazia
        } finally {
            if (proc != null && proc.isAlive()) // se o processo ainda estiver vivo, mata ele forçadamente
                proc.destroyForcibly();
        }
        return linhas; // devolve a lista de linhas do output do comando
    }
}
