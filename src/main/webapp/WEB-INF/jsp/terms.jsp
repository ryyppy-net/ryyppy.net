<%@page contentType="text/html" pageEncoding="UTF-8" isELIgnored="false"%>
<%@taglib uri="jakarta.tags.core" prefix="c" %>
<%@taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:master title="Käyttöehdot - Ryyppy.net">
    <jsp:attribute name="customHead">
        <link rel="stylesheet" type="text/css" href="/static/css/style.css" />
        <link rel="stylesheet" type="text/css" href="/static/css/login.css" />
        <style>
            .terms {
                background-color: #000000;
                border: 3px solid #505050;
                border-radius: 10px;
                margin: 20px auto;
                padding: 20px 30px;
                color: #ffffff;
                max-width: 800px;
                text-align: left;
                line-height: 1.6;
            }
            .terms h1 {
                text-align: center;
                margin-bottom: 30px;
                font-size: 1.8em;
            }
            .terms h2 {
                margin-top: 25px;
                margin-bottom: 15px;
                font-size: 1.3em;
                color: #ffffff;
                border-bottom: 1px solid #505050;
                padding-bottom: 5px;
            }
            .terms p, .terms li {
                color: #cccccc;
                margin-bottom: 10px;
            }
            .terms ul {
                margin-left: 20px;
                margin-bottom: 15px;
            }
            .terms a {
                color: #aaaaaa;
                text-decoration: underline;
            }
            .terms a:hover {
                color: #ffffff;
            }
            .back-link {
                text-align: center;
                margin: 20px;
            }
            .back-link a {
                color: #aaaaaa;
                text-decoration: none;
            }
            .last-updated {
                text-align: center;
                color: #888888;
                font-size: 0.9em;
                margin-top: 30px;
            }
            .warning-box {
                background-color: #3a2a00;
                border: 1px solid #665500;
                border-radius: 5px;
                padding: 15px;
                margin: 20px 0;
            }
            .warning-box p {
                color: #ffcc00;
                margin: 0;
            }
        </style>
    </jsp:attribute>

    <jsp:body>
        <div id="logoContainer">
            <a href="/ui/login">
                <img id="logo" src="/static/images/logo_ryyppy.png" alt="Ryyppy.net" title="Ryyppy.net" style="max-width: 300px;" />
            </a>
        </div>

        <div class="terms">
            <h1>Käyttöehdot</h1>

            <h2>1. Ehtojen hyväksyminen</h2>
            <p>
                Käyttämällä Ryyppy.net-palvelua hyväksyt nämä käyttöehdot.
                Jos et hyväksy näitä ehtoja, älä käytä palvelua.
            </p>

            <h2>2. Palvelun kuvaus</h2>
            <p>
                Ryyppy.net on alkoholinkulutuksen seurantapalvelu, joka laskee arvioitua
                veren alkoholipitoisuutta (promilleja) käyttäjän antamien tietojen perusteella.
            </p>

            <div class="warning-box">
                <p>
                    <strong>TÄRKEÄ VAROITUS:</strong> Promillelaskelmat ovat vain arvioita eivätkä korvaa
                    virallisia alkometrimittauksia. Älä koskaan aja autoa tai käytä koneita alkoholin
                    nauttimisen jälkeen, riippumatta palvelun näyttämistä arvoista.
                </p>
            </div>

            <h2>3. Käyttäjätilit</h2>
            <p>Rekisteröityessäsi palveluun sitoudut:</p>
            <ul>
                <li>Pitämään kirjautumistietosi salassa</li>
                <li>Ilmoittamaan välittömästi, jos epäilet tilisi väärinkäyttöä</li>
            </ul>
            <p>
                Olet vastuussa kaikesta toiminnasta, joka tapahtuu tililläsi.
            </p>
            <p>
                Huomioithan, että promillelaskelmat perustuvat antamiisi tietoihin (paino, sukupuoli).
                Jos syötät epätarkkoja arvoja, laskelmat eivät vastaa todellista tilaasi.
            </p>

            <h2>4. Ikäraja</h2>
            <p>
                Palvelu on tarkoitettu vain täysi-ikäisille (18+).
                Rekisteröitymällä vahvistat olevasi vähintään 18-vuotias.
            </p>

            <h2>5. Hyväksyttävä käyttö</h2>
            <p>Sitoudut olemaan:</p>
            <ul>
                <li>Käyttämättä palvelua laittomiin tarkoituksiin</li>
                <li>Häiritsemättä palvelun toimintaa</li>
                <li>Yrittämättä päästä käsiksi muiden käyttäjien tietoihin</li>
                <li>Levittämättä haittaohjelmia tai haitallista sisältöä</li>
            </ul>

            <h2>6. Vastuunrajoitus</h2>
            <p>
                <strong>Palvelu tarjotaan "sellaisenaan" ilman takuita.</strong>
            </p>
            <p>Ryyppy.net ei vastaa:</p>
            <ul>
                <li>Promillelaskelmien tarkkuudesta tai oikeellisuudesta</li>
                <li>Palvelun käyttöön perustuvista päätöksistä</li>
                <li>Välillisistä tai välittömistä vahingoista</li>
                <li>Palvelun keskeytyksistä tai tietojen menetyksestä</li>
            </ul>

            <div class="warning-box">
                <p>
                    Promillelaskelmat ovat teoreettisia arvioita, joihin vaikuttavat monet tekijät
                    kuten ruokailu, lääkitys, terveydentila ja yksilöllinen aineenvaihdunta.
                    Todelliset arvot voivat poiketa merkittävästi.
                </p>
            </div>

            <h2>7. Vastuullinen alkoholinkäyttö</h2>
            <p>
                Ryyppy.net kannustaa vastuulliseen alkoholinkäyttöön.
                Palvelun tarkoitus on auttaa seuraamaan kulutusta, ei kannustaa liialliseen juomiseen.
            </p>
            <p>
                Jos sinulla on ongelmia alkoholinkäytön kanssa, ota yhteyttä:
            </p>
            <ul>
                <li>A-klinikka: <a href="https://a-klinikkasaatio.fi">a-klinikkasaatio.fi</a></li>
                <li>Päihdelinkki: <a href="https://paihdelinkki.fi">paihdelinkki.fi</a></li>
            </ul>

            <h2>8. Immateriaalioikeudet</h2>
            <p>
                Palvelun sisältö, mukaan lukien logo, tekstit ja ohjelmistokoodi,
                on suojattu tekijänoikeuksin. Käyttäjällä on oikeus käyttää palvelua
                henkilökohtaisiin tarkoituksiin näiden ehtojen mukaisesti.
            </p>

            <h2>9. Tilin päättäminen</h2>
            <p>
                Voit milloin tahansa lopettaa palvelun käytön ja pyytää tilisi poistamista.
                Pidätämme oikeuden sulkea tilejä, jotka rikkovat näitä käyttöehtoja.
            </p>

            <h2>10. Muutokset käyttöehtoihin</h2>
            <p>
                Pidätämme oikeuden päivittää näitä käyttöehtoja.
                Merkittävistä muutoksista ilmoitetaan palvelussa.
                Jatkamalla palvelun käyttöä muutosten jälkeen hyväksyt päivitetyt ehdot.
            </p>

            <h2>11. Sovellettava laki</h2>
            <p>
                Näihin käyttöehtoihin sovelletaan Suomen lakia.
                Mahdolliset riidat ratkaistaan Suomen tuomioistuimissa.
            </p>

            <h2>12. Tietosuoja</h2>
            <p>
                Henkilötietojesi käsittelyä koskevat tiedot löytyvät
                <a href="/ui/privacy">tietosuojaselosteestamme</a>.
            </p>

            <h2>13. Yhteydenotto</h2>
            <p>
                Kysymykset käyttöehdoista:<br>
                Sähköposti: privacy@ryyppy.net
            </p>

            <p class="last-updated">Päivitetty: 17.1.2026</p>
        </div>

        <div class="back-link">
            <a href="/ui/login">&larr; Takaisin kirjautumissivulle</a>
        </div>
    </jsp:body>
</t:master>
